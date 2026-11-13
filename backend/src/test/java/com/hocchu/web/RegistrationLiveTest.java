package com.hocchu.web;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="liveBackend", matches="true")
class RegistrationLiveTest extends LocalBackendFixture {
    @Test void eachPublicRoleCanRegisterThenSignInWithoutOtp() throws Exception {
        for(String role:List.of("STUDENT","PARENT","TEACHER")) {
            String name="signup_"+UUID.randomUUID().toString().replace("-","").substring(0,18);
            var signup=json.createObjectNode().put("username",name).put("email",name+"@example.invalid")
                .put("password"," test-password ").put("fullName","Đăng ký kiểm thử").put("role",role);
            var response=request("/api/auth/signup","POST",null,signup);
            long id;
            try(var p=db.prepareStatement("SELECT id,enabled,otp_code,otp_expiry FROM users WHERE username=?")) {
                p.setString(1,name);try(var rows=p.executeQuery()) {
                    assertTrue(rows.next());id=rows.getLong("id");created.add(id);
                    assertTrue(rows.getBoolean("enabled"));assertNull(rows.getString("otp_code"));assertNull(rows.getTimestamp("otp_expiry"));
                }
            }
            assertEquals(200,response.statusCode());assertTrue(json.readTree(response.body()).path("success").asBoolean());
            var login=ok("/api/auth/signin","POST",null,json.createObjectNode().put("usernameOrEmail",name).put("password"," test-password "));
            String token=login.path("accessToken").asText();assertFalse(token.isBlank());
            var me=ok("/api/auth/me","GET",token,null);assertEquals(id,me.path("id").asLong());
            assertTrue(me.path("roles").toString().contains("ROLE_"+role));assertFalse(me.path("roles").toString().contains("ROLE_ADMIN"));
            if(role.equals("TEACHER")) {
                assertTrue(me.path("roles").toString().contains("PERM_ASSIGN_HOMEWORK"));
                var classroom=ok("/api/teacher/class?className=Signup%20fixture","POST",token,null);
                createdClasses.add(classroom.path("id").asLong());assertTrue(createdClasses.get(createdClasses.size()-1)>0);
            }
            assertEquals(400,request("/api/auth/signup","POST",null,signup).statusCode());
        }
    }
    @Test void publicCannotCreateAdminOrActivateAnExistingLockedUserThroughLegacyOtp() throws Exception {
        var payload=json.createObjectNode().put("username","not-created").put("email","not-created@example.invalid")
            .put("password","test-password").put("fullName","Không tạo").put("role","ADMIN");
        assertEquals(400,request("/api/auth/signup","POST",null,payload).statusCode());
        assertEquals(401,request("/api/auth/verify-otp","POST",null,json.createObjectNode().put("emailOrUsername","not-created").put("otpCode","123456")).statusCode());
    }
}
