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
        try(var p=db.prepareStatement("INSERT INTO users(username,password,full_name,enabled) SELECT ?,password,?,TRUE FROM users WHERE username='student1'",Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,name);p.setString(2,"Kiểm thử phụ huynh");assertEquals(1,p.executeUpdate());
            try(var rs=p.getGeneratedKeys()) {assertTrue(rs.next());created.add(rs.getLong(1));}
        }
        long id=created.get(created.size()-1);
        try(var p=db.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name=?")) {p.setLong(1,id);p.setString(2,role);assertEquals(1,p.executeUpdate());}
        String token=ok("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",name).put("password","123456")).path("accessToken").asText();
        assertFalse(token.isBlank());return new Account(id,token);
    }
    @BeforeEach void connect() throws Exception {
        Properties env=new Properties();try(var in=Files.newInputStream(Path.of(".env.local"))) {env.load(in);}
        db=DriverManager.getConnection("jdbc:mysql://localhost:3306/viet_letters_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",env.getProperty("DB_USERNAME","root"),env.getProperty("DB_PASSWORD"));
    }
    @AfterEach void cleanup() throws Exception {
        if(db==null) return;
        try {
            for(long id:created) {
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
}
