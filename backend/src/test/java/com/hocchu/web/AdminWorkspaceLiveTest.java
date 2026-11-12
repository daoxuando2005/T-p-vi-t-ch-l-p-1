package com.hocchu.web;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.sql.Statement;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="liveBackend",matches="true")
class AdminWorkspaceLiveTest extends LocalBackendFixture {
    @Test void curriculumStorageRoundTripsThroughMySqlWithoutCommittingChanges() throws Exception {
        String before;try(var p=db.prepareStatement("SELECT practices FROM student_learning_content WHERE letter='A'");var r=p.executeQuery()){assertTrue(r.next());before=r.getString(1);}
        db.setAutoCommit(false);
        try {
            assertFalse(db.getAutoCommit());
            var operations=new com.hocchu.web.service.AdminOperationsService();
            var template=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(db,true));
            org.springframework.test.util.ReflectionTestUtils.setField(operations,"jdbc",template);
            var p=new com.hocchu.web.service.AdminOperationsService.Practice("ba","Hình ba",List.of("Bé","yêu","ba."));
            operations.saveCurriculum("A",new com.hocchu.web.service.AdminOperationsService.Curriculum(List.of(p,p,p)));
            String stored=template.queryForObject("SELECT practices FROM student_learning_content WHERE letter='A'",String.class);
            var parsed=json.readTree(stored);assertEquals(3,parsed.size());assertEquals("ba",parsed.get(0).path("text").asText());assertEquals(3,parsed.get(0).path("sentence").size());
        } finally {db.rollback();db.setAutoCommit(true);}
        try(var p=db.prepareStatement("SELECT practices FROM student_learning_content WHERE letter='A'");var r=p.executeQuery()){assertTrue(r.next());assertEquals(before,r.getString(1));}
    }
    record Managed(long id,String username,String password,String token) {}
    Managed managed(Account admin,String role) throws Exception {
        String name="admin_"+UUID.randomUUID().toString().replace("-","").substring(0,12),password="test-"+UUID.randomUUID();
        var body=json.createObjectNode().put("username",name).put("password",password).put("fullName","Tài khoản kiểm thử").put("email",name+"@example.invalid").put("role",role);
        assertTrue(ok("/api/admin/users","POST",admin.token(),body).path("success").asBoolean());long id;
        try(var p=db.prepareStatement("SELECT id FROM users WHERE username=?")){p.setString(1,name);try(var rows=p.executeQuery()){assertTrue(rows.next());id=rows.getLong(1);created.add(id);}}
        return new Managed(id,name,password,signIn(name,password));
    }
    String signIn(String name,String password) throws Exception {
        return ok("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",name).put("password",password)).path("accessToken").asText();
    }
    @Test void accountActionsRevokeOnlyTargetSessionsAndCannotEscalateThroughPermissions() throws Exception {
        var admin=account("ROLE_ADMIN");var student=managed(admin,"STUDENT");var teacher=managed(admin,"TEACHER");
        assertEquals(403,request("/api/admin/users","GET",student.token(),null).statusCode());
        assertEquals(403,request("/api/admin/settings","GET",teacher.token(),null).statusCode());
        assertEquals(400,request("/api/admin/users/"+admin.id()+"/toggle","PUT",admin.token(),null).statusCode());
        ok("/api/admin/users/"+student.id()+"/toggle","PUT",admin.token(),null);
        assertEquals(401,request("/api/auth/me","GET",student.token(),null).statusCode());
        assertEquals(401,request("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",student.username()).put("password",student.password())).statusCode());
        ok("/api/admin/users/"+student.id()+"/toggle","PUT",admin.token(),null);
        assertEquals(401,request("/api/auth/me","GET",student.token(),null).statusCode());
        String fresh=signIn(student.username(),student.password());assertEquals(200,request("/api/auth/me","GET",fresh,null).statusCode());
        String password="new-test-"+UUID.randomUUID();ok("/api/admin/users/"+student.id()+"/password","POST",admin.token(),json.createObjectNode().put("password",password));
        assertEquals(401,request("/api/auth/me","GET",fresh,null).statusCode());
        assertEquals(401,request("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",student.username()).put("password",student.password())).statusCode());
        assertFalse(signIn(student.username(),password).isBlank());
        assertEquals(400,request("/api/admin/users/"+teacher.id()+"/permissions","PUT",admin.token(),json.createArrayNode().add("ROLE_ADMIN")).statusCode());
        ok("/api/admin/users/"+teacher.id()+"/permissions","PUT",admin.token(),json.createArrayNode().add("PERM_VIEW_ALPHABET"));
        assertEquals(401,request("/api/auth/me","GET",teacher.token(),null).statusCode());
        String limitedTeacher=signIn(teacher.username(),teacher.password());
        var profile=ok("/api/auth/me","GET",limitedTeacher,null);
        assertTrue(profile.path("roles").toString().contains("PERM_VIEW_ALPHABET"));assertFalse(profile.path("roles").toString().contains("PERM_ASSIGN_HOMEWORK"));
        assertEquals(403,request("/api/teacher/class?className=DeniedFixture","POST",limitedTeacher,null).statusCode());
        assertEquals(200,request("/api/admin/settings","GET",admin.token(),null).statusCode());
    }
    @Test void unusedAccountCanChangeRoleOrBeDeletedWhileClassHistoryStaysProtected() throws Exception {
        var admin=account("ROLE_ADMIN");var student=managed(admin,"STUDENT");var teacher=managed(admin,"TEACHER");
        ok("/api/admin/users/"+student.id()+"/role?role=PARENT","PUT",admin.token(),null);
        assertEquals(401,request("/api/auth/me","GET",student.token(),null).statusCode());
        assertTrue(ok("/api/auth/me","GET",signIn(student.username(),student.password()),null).path("roles").toString().contains("ROLE_PARENT"));
        var school=ok("/api/teacher/class?className=AdminFixture","POST",teacher.token(),null);long classId=school.path("id").asLong();createdClasses.add(classId);
        assertEquals(400,request("/api/admin/users/"+teacher.id(),"DELETE",admin.token(),null).statusCode());
        assertEquals(400,request("/api/admin/users/"+teacher.id()+"/role?role=STUDENT","PUT",admin.token(),null).statusCode());
        assertEquals(classId,ok("/api/admin/classes/"+classId,"GET",admin.token(),null).path("id").asLong());
        assertEquals(403,request("/api/admin/classes/"+classId,"GET",teacher.token(),null).statusCode());
        ok("/api/admin/users/"+student.id(),"DELETE",admin.token(),null);created.remove(student.id());
        assertEquals(401,request("/api/auth/me","GET",student.token(),null).statusCode());
    }
    @Test void familySupportRemovesOnlyChosenLinkAndDoesNotExposeInvitationOrSecrets() throws Exception {
        var admin=account("ROLE_ADMIN");var child=account("ROLE_STUDENT");var parent=account("ROLE_PARENT");
        long linkId;try(var p=db.prepareStatement("INSERT INTO parent_student_links(parent_id,student_id,link_code,status) VALUES(?,?,?,'APPROVED')",Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1,parent.id());p.setLong(2,child.id());p.setString(3,"test"+UUID.randomUUID().toString().substring(0,12));p.executeUpdate();try(var keys=p.getGeneratedKeys()){assertTrue(keys.next());linkId=keys.getLong(1);}}
        String dashboard="/api/parent/students/"+child.id()+"/dashboard";
        assertEquals(200,request(dashboard,"GET",parent.token(),null).statusCode());
        var links=ok("/api/admin/family-links","GET",admin.token(),null);assertFalse(links.toString().contains("linkCode"));
        ok("/api/admin/family-links/"+linkId,"DELETE",admin.token(),null);
        assertEquals(403,request(dashboard,"GET",parent.token(),null).statusCode());
        assertEquals(29,ok("/api/student/learning/progress","GET",child.token(),null).size());
        var config=ok("/api/admin/settings","GET",admin.token(),null);assertTrue(config.has("aiConfigured"));assertFalse(config.has("apiKey"));assertFalse(config.path("chatEnabled").asBoolean());
        var stats=ok("/api/admin/stats","GET",admin.token(),null);assertEquals(7,stats.path("days").size());assertTrue(stats.path("totalUsers").asLong()>=3);
        var lessons=ok("/api/student/alphabet","GET",admin.token(),null);
        assertEquals(400,request("/api/admin/alphabet/"+lessons.get(0).path("id").asLong(),"DELETE",admin.token(),null).statusCode());
        var before=ok("/api/admin/learning-content","GET",admin.token(),null);
        assertEquals(400,request("/api/admin/learning-content/A","PUT",admin.token(),json.createObjectNode().set("practices",json.createArrayNode())).statusCode());
        assertEquals(before,ok("/api/admin/learning-content","GET",admin.token(),null));
    }
}
