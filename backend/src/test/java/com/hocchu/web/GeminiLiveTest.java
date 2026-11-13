package com.hocchu.web;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit real-provider check; only generic lesson prompts and disposable teacher identity. */
@EnabledIfSystemProperty(named="liveGemini", matches="true")
class GeminiLiveTest extends LocalBackendFixture {
    @Test void createsReviewableDraftsForAllThreeAssignmentTypes() throws Exception {
        var teacher=account("ROLE_TEACHER");
        assertTrue(ok("/api/teacher/assistant/status","GET",teacher.token(),null).path("configured").asBoolean());
        for(String type:List.of("WRITING","QUIZ","SENTENCE")) {
            String prompt=switch(type) {case "WRITING"->"Cho bé lớp 1 tập viết chữ A.";case "QUIZ"->"Một câu điền chữ còn thiếu trong từ ba, lựa chọn chữ a hoặc o.";default->"Một câu ghép từ: Bé yêu mẹ.";};
            var payload=json.createObjectNode().put("message",prompt).put("type",type).put("count",1);payload.putArray("history");
            var request=HttpRequest.newBuilder(URI.create(base+"/api/teacher/assistant/draft")).timeout(Duration.ofSeconds(45))
                .header("Authorization","Bearer "+teacher.token()).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            assertEquals(200,response.statusCode(),"Gemini draft HTTP status for "+type);
            var body=json.readTree(response.body());assertEquals(type,body.path("draft").path("contentType").asText());
            assertFalse(body.path("draft").path("title").asText().isBlank());assertFalse(body.path("draft").path("contentData").asText().isBlank());
        }
        try(var p=db.prepareStatement("SELECT COUNT(*) FROM assignments a JOIN classes c ON a.class_id=c.id WHERE c.teacher_id=?")) {
            p.setLong(1,teacher.id());try(var r=p.executeQuery()){assertTrue(r.next());assertEquals(0,r.getLong(1));}
        }
    }
}
