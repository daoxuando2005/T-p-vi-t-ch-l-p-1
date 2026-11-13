package com.hocchu.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated local fixtures; cleanup is limited to IDs created by the test. */
abstract class LocalBackendFixture {
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    final String base=System.getProperty("backendUrl","http://127.0.0.1:8080");
    final List<Long> created=new ArrayList<>();
    final List<Long> createdClasses=new ArrayList<>();
    final List<Path> createdMedia=new ArrayList<>();
    Connection db;
    record Account(long id,String token,String username,String password) {
        @Override public String toString(){return "Account(id="+id+", credentials=[redacted])";}
    }
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
        if(role.equals("ROLE_TEACHER"))try(var p=db.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name IN ('PERM_ASSIGN_HOMEWORK','PERM_MANAGE_CLASSES','PERM_VIEW_ALPHABET','PERM_EDIT_ALPHABET')")){p.setLong(1,id);p.executeUpdate();}
        String token=ok("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",name).put("password",password)).path("accessToken").asText();
        assertFalse(token.isBlank());return new Account(id,token,name,password);
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
}
