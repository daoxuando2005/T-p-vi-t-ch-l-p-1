package com.hocchu.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.hocchu.web.service.LearningTrackingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in check against the local running server. Only deletes users created by this test. */
@EnabledIfSystemProperty(named="liveBackend",matches="true")
class ParentTrackingLiveTest {
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final String base=System.getProperty("backendUrl","http://127.0.0.1:8080");
    final List<Long> created=new ArrayList<>();
    final List<Long> createdClasses=new ArrayList<>();
    final List<Path> createdMedia=new ArrayList<>();
    Connection db;
    record Account(long id,String token) {}
    HttpResponse<String> request(String path,String method,String token,JsonNode body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(15));
        if(token!=null) b.header("Authorization","Bearer "+token);
        b.header("Content-Type","application/json");
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode ok(String path,String method,String token,JsonNode body) throws Exception {
        var r=request(path,method,token,body);assertEquals(200,r.statusCode(),"Unexpected HTTP status at "+path.split("\\?")[0]);return json.readTree(r.body());
    }
    Account account(String role) throws Exception {
        String name="parent_flow_"+UUID.randomUUID().toString().replace("-","").substring(0,15);
        String password="test-"+UUID.randomUUID();String encoded=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password);
        try(var p=db.prepareStatement("INSERT INTO users(username,password,full_name,enabled) VALUES(?,?,?,TRUE)",Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,name);p.setString(2,encoded);p.setString(3,"Kiểm thử phụ huynh");assertEquals(1,p.executeUpdate());
            try(var rs=p.getGeneratedKeys()) {assertTrue(rs.next());created.add(rs.getLong(1));}
        }
        long id=created.get(created.size()-1);
        try(var p=db.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name=?")) {p.setLong(1,id);p.setString(2,role);assertEquals(1,p.executeUpdate());}
        String token=ok("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",name).put("password",password)).path("accessToken").asText();
        assertFalse(token.isBlank());return new Account(id,token);
    }
    @BeforeEach void connect() throws Exception {
        Properties env=new Properties();try(var in=Files.newInputStream(Path.of(".env.local"))) {env.load(in);}
        db=DriverManager.getConnection("jdbc:mysql://localhost:3306/viet_letters_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",env.getProperty("DB_USERNAME","root"),env.getProperty("DB_PASSWORD"));
    }
    @AfterEach void cleanup() throws Exception {
        if(db==null) return;
        try {
            for(Path media:createdMedia){assertTrue(media.toAbsolutePath().normalize().startsWith(Path.of("uploads/example_images").toAbsolutePath().normalize()));Files.deleteIfExists(media);}
            for(long classId:createdClasses) {
                try(var p=db.prepareStatement("SELECT teacher_id FROM classes WHERE id=?")){p.setLong(1,classId);try(var r=p.executeQuery()){assertTrue(r.next());assertTrue(created.contains(r.getLong(1)));}}
                try(var p=db.prepareStatement("DELETE FROM submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE class_id=?)")){p.setLong(1,classId);p.executeUpdate();}
                for(String sql:List.of("DELETE FROM assignments WHERE class_id=?","DELETE FROM class_members WHERE class_id=?","DELETE FROM classes WHERE id=?"))
                    try(var p=db.prepareStatement(sql)){p.setLong(1,classId);p.executeUpdate();}
            }
            for(long id:created) {
                try(var p=db.prepareStatement("DELETE FROM notifications WHERE user_id=?")){p.setLong(1,id);p.executeUpdate();}
                try(var p=db.prepareStatement("DELETE FROM parent_student_links WHERE student_id=? OR parent_id=?")) {p.setLong(1,id);p.setLong(2,id);p.executeUpdate();}
                try(var p=db.prepareStatement("DELETE FROM user_roles WHERE user_id=?")) {p.setLong(1,id);p.executeUpdate();}
                try(var p=db.prepareStatement("DELETE FROM users WHERE id=?")) {p.setLong(1,id);assertEquals(1,p.executeUpdate());}
            }
        } finally {db.close();}
    }
    ObjectNode snapshot(int completed) {
        var body=json.createObjectNode();var progress=body.putArray("progress");body.putArray("events");
        for(int i=0;i<29;i++) {boolean done=i<completed;progress.addObject().put("letter",LearningTrackingService.LETTERS.get(i)).put("read",done).put("writes",done?2:0).put("words",done?3:0).put("sentences",done?3:0);}
        return body;
    }
    void event(ArrayNode events,String kind,String letter,Instant at) {
        events.addObject().put("id",UUID.randomUUID().toString()).put("kind",kind).put("letter",letter).put("source","FREE").put("review",false).put("occurredAt",at.toString());
    }
    @Test void familyFlowUsesApprovedLinksAndIdempotentVietnamDays() throws Exception {
        var child=account("ROLE_STUDENT");var parent=account("ROLE_PARENT");var other=account("ROLE_PARENT");
        String dashboard="/api/parent/students/"+child.id()+"/dashboard";
        assertEquals(403,request(dashboard,"GET",parent.token(),null).statusCode());
        assertEquals(400,request("/api/parent/link?linkCode=student1","POST",parent.token(),null).statusCode());
        String code=ok("/api/student/link-code","GET",child.token(),null).path("linkCode").asText();assertEquals(22,code.length());
        String link="/api/parent/link?linkCode="+URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(ok(link,"POST",parent.token(),null).path("success").asBoolean());
        assertEquals(400,request(link,"POST",other.token(),null).statusCode());
        assertTrue(ok("/api/student/link-code","GET",child.token(),null).path("linked").asText().equals("true"));
        assertEquals(403,request(dashboard,"GET",other.token(),null).statusCode());
        assertEquals(403,request(dashboard,"GET",child.token(),null).statusCode());
        var day=LocalDate.now(LearningTrackingService.SCHOOL_ZONE);var start=day.atStartOfDay(LearningTrackingService.SCHOOL_ZONE).toInstant();
        var body=snapshot(3);var events=(ArrayNode)body.get("events");
        event(events,"READING","A",start.plusSeconds(1).plusNanos(123456789));event(events,"WRITING","Ă",start.minusSeconds(1));event(events,"WRITING","Â",start.plusSeconds(2));
        String sync="/api/student/learning/sync";
        assertEquals(3,ok(sync,"POST",child.token(),body).path("acceptedIds").size());
        ok(sync,"POST",child.token(),body); // Retried batch must not duplicate activities.
        ok(sync,"POST",child.token(),snapshot(0)); // Old device must not regress earned progress.
        var data=ok(dashboard+"?date="+day,"GET",parent.token(),null);
        assertEquals(29,data.path("progress").size());assertEquals(3,data.path("completedLetters").asInt());
        assertEquals(1,data.path("summary").path("readings").asInt());assertEquals(1,data.path("summary").path("writings").asInt());assertEquals(2,data.path("timeline").size());
        assertEquals(1,ok(dashboard+"?date="+day.minusDays(1),"GET",parent.token(),null).path("summary").path("writings").asInt());
        assertEquals(400,request(dashboard+"?date="+day.plusDays(1),"GET",parent.token(),null).statusCode());
        assertEquals(401,request(dashboard,"GET",null,null).statusCode());
        // Missing handlers may reach Spring Security's protected /error dispatch.
        assertTrue(Set.of(403,404).contains(request("/api/chat/contacts","GET",parent.token(),null).statusCode()));
        assertTrue(Set.of(403,404).contains(request("/ws/info","GET",parent.token(),null).statusCode()));
    }
    @Test void pendingPlaceholderDoesNotLeakAndExpiredCodeCannotClaim() throws Exception {
        var child=account("ROLE_STUDENT");var parent=account("ROLE_PARENT");var placeholder=account("ROLE_PARENT");
        try(var p=db.prepareStatement("INSERT INTO parent_student_links(parent_id,student_id,link_code,status) VALUES(?,?,?,'PENDING')")) {p.setLong(1,placeholder.id());p.setLong(2,child.id());p.setString(3,"test"+UUID.randomUUID().toString().substring(0,12));p.executeUpdate();}
        String path="/api/parent/students/"+child.id()+"/dashboard";
        assertEquals(403,request(path,"GET",placeholder.token(),null).statusCode());
        String code=ok("/api/student/link-code","GET",child.token(),null).path("linkCode").asText();
        try(var p=db.prepareStatement("UPDATE parent_link_invites SET expires_at_utc='2000-01-01' WHERE student_id=?")) {p.setLong(1,child.id());p.executeUpdate();}
        assertEquals(400,request("/api/parent/link?linkCode="+code,"POST",parent.token(),null).statusCode());
        code=ok("/api/student/link-code","GET",child.token(),null).path("linkCode").asText();
        ok("/api/parent/link?linkCode="+code,"POST",parent.token(),null);
        assertEquals(403,request(path,"GET",placeholder.token(),null).statusCode());
        assertEquals(200,request(path,"GET",parent.token(),null).statusCode());
    }
    @Test void contentRestoreAndSubmissionReviewsWorkAcrossStudentTeacherAndParent() throws Exception {
        var teacher=account("ROLE_TEACHER");var child=account("ROLE_STUDENT");var otherChild=account("ROLE_STUDENT");
        var parent=account("ROLE_PARENT");var otherParent=account("ROLE_PARENT");
        var content=ok("/api/student/content","GET",child.token(),null);assertEquals(29,content.size());
        assertEquals("A",content.get(0).path("letter").asText());assertEquals("Ă",content.get(1).path("letter").asText());
        for(var letter:content)assertEquals(3,letter.path("practices").size());
        assertEquals(403,request("/api/student/content","GET",parent.token(),null).statusCode());
        ok("/api/student/learning/sync","POST",child.token(),snapshot(3));
        var restored=ok("/api/student/learning/progress","GET",child.token(),null);assertEquals(29,restored.size());assertEquals(3,restored.get(2).path("sentences").asInt());
        assertFalse(ok("/api/student/learning/progress","GET",otherChild.token(),null).get(0).path("read").asBoolean());
        String code=ok("/api/student/link-code","GET",child.token(),null).path("linkCode").asText();ok("/api/parent/link?linkCode="+code,"POST",parent.token(),null);
        // Old pending placeholders must neither break submissions nor receive notifications.
        try(var p=db.prepareStatement("INSERT INTO parent_student_links(parent_id,student_id,link_code,status) VALUES(?,?,?,'PENDING')")) {
            p.setLong(1,otherParent.id());p.setLong(2,child.id());p.setString(3,"test"+UUID.randomUUID().toString().substring(0,12));p.executeUpdate();
        }
        var school=ok("/api/teacher/class?className="+URLEncoder.encode("Lớp kiểm thử "+UUID.randomUUID(),java.nio.charset.StandardCharsets.UTF_8),"POST",teacher.token(),null);
        long classId=school.path("id").asLong();assertTrue(classId>0);createdClasses.add(classId);
        ok("/api/student/class/join?inviteCode="+school.path("inviteCode").asText(),"POST",child.token(),null);
        var quiz=json.createObjectNode().put("classId",classId).put("title","Bài trắc nghiệm kiểm thử").put("contentType","QUIZ")
            .put("contentData","[{\"question\":\"Chọn A\",\"options\":[\"A\",\"B\"],\"answer\":\"A\"}]");
        long quizId=ok("/api/teacher/assignments","POST",teacher.token(),quiz).path("id").asLong();
        ok("/api/student/assignments/"+quizId+"/submit","POST",child.token(),json.createObjectNode().put("submissionData","{\"answers\":[\"A\"]}"));
        var review=ok("/api/student/assignments/"+quizId+"/submission","GET",child.token(),null);assertEquals(10,review.path("score").asInt());
        assertEquals(403,request("/api/student/assignments/"+quizId+"/submission","GET",otherChild.token(),null).statusCode());
        String parentPath="/api/parent/students/"+child.id()+"/submissions/"+review.path("id").asLong();
        assertEquals(200,request(parentPath,"GET",parent.token(),null).statusCode());assertEquals(403,request(parentPath,"GET",otherParent.token(),null).statusCode());
        var writing=json.createObjectNode().put("classId",classId).put("title","Bài viết kiểm thử").put("contentType","WRITING").put("contentData","A");
        long writingId=ok("/api/teacher/assignments","POST",teacher.token(),writing).path("id").asLong();
        var image=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,64,64);g.setColor(java.awt.Color.BLUE);g.drawLine(15,50,30,10);g.drawLine(30,10,48,50);g.dispose();
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        String data=json.createArrayNode().add(json.createObjectNode().put("letter","A").put("image","data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes.toByteArray()))).toString();
        var submitted=json.createObjectNode().put("submissionData",data);String submit="/api/student/assignments/"+writingId+"/submit";
        var calls=new ArrayList<java.util.concurrent.CompletableFuture<HttpResponse<String>>>();
        for(int i=0;i<2;i++)calls.add(http.sendAsync(HttpRequest.newBuilder(URI.create(base+submit)).header("Content-Type","application/json").header("Authorization","Bearer "+child.token()).POST(HttpRequest.BodyPublishers.ofString(submitted.toString())).build(),HttpResponse.BodyHandlers.ofString()));
        for(var call:calls)assertEquals(200,call.get(20,java.util.concurrent.TimeUnit.SECONDS).statusCode());
        var saved=ok("/api/student/assignments/"+writingId+"/submission","GET",child.token(),null);assertTrue(saved.path("score").isNull());assertEquals(data,saved.path("submissionData").asText());
        long submissionId=saved.path("id").asLong();
        try(var p=db.prepareStatement("SELECT COUNT(*) FROM submissions WHERE assignment_id=? AND student_id=?")){p.setLong(1,writingId);p.setLong(2,child.id());try(var r=p.executeQuery()){assertTrue(r.next());assertEquals(1,r.getInt(1));}}
        ok("/api/teacher/submissions/"+submissionId+"/grade","POST",teacher.token(),json.createObjectNode().put("score",8.5).put("feedback","Cô đã xem bài viết"));
        ok(submit,"POST",child.token(),submitted);
        var graded=ok("/api/parent/students/"+child.id()+"/submissions/"+submissionId,"GET",parent.token(),null);assertEquals(8.5,graded.path("score").asDouble());assertEquals("Cô đã xem bài viết",graded.path("feedback").asText());
        try(var p=db.prepareStatement("SELECT COUNT(*) FROM notifications WHERE user_id=?")){p.setLong(1,otherParent.id());try(var r=p.executeQuery()){assertTrue(r.next());assertEquals(0,r.getInt(1));}}
        assertEquals(400,request("/api/teacher/submissions/"+submissionId+"/grade","POST",teacher.token(),json.createObjectNode().put("score",12)).statusCode());
        ok("/api/parent/students/"+child.id()+"/link","DELETE",parent.token(),null);
        assertEquals(403,request(parentPath,"GET",parent.token(),null).statusCode());
        assertEquals(3,ok("/api/student/learning/progress","GET",child.token(),null).get(2).path("sentences").asInt());
    }
    @Test void uploadedTeacherMediaIsServedByBackendWithoutAWebFrontend() throws Exception {
        var teacher=account("ROLE_TEACHER");String filename="student-parent-test-"+UUID.randomUUID()+".png";
        Path stored=Path.of("uploads/example_images",filename);createdMedia.add(stored);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        String boundary="test-"+UUID.randomUUID();var body=new java.io.ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+filename+"\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(bytes.toByteArray());body.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var upload=http.send(HttpRequest.newBuilder(URI.create(base+"/api/teacher/upload-image?filename="+filename))
            .header("Authorization","Bearer "+teacher.token()).header("Content-Type","multipart/form-data; boundary="+boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,upload.statusCode());String url=json.readTree(upload.body()).path("message").asText();assertTrue(url.startsWith("/api/public/media/example_images/"));
        var image=http.send(HttpRequest.newBuilder(URI.create(base+url)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,image.statusCode());assertArrayEquals(bytes.toByteArray(),image.body());
    }
}
