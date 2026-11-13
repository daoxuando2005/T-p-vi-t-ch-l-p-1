package com.hocchu.web;
import com.hocchu.web.service.TeacherAssistantService;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GeminiAssistantTest {
    HttpServer server; String base, draft, reason, block; int status, calls; JsonNode received;
    final ObjectMapper json=new ObjectMapper();
    @BeforeEach void start() throws Exception {
        status=200;reason="STOP";block="";
        draft="""
            {"reply":"Cô duyệt bài nhé.","draft":{"title":"Gia đình","description":"Luyện ghép câu","contentType":"SENTENCE",
            "content":{"sentences":[{"prompt":"Ghép câu đúng","words":["Bé","yêu","mẹ."]}]}}}
            """;
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);base="http://127.0.0.1:"+server.getAddress().getPort();
        server.createContext("/models/fixture-model:generateContent",exchange->{
            calls++;assertEquals("fixture-key",exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));assertNull(exchange.getRequestURI().getQuery());
            received=json.readTree(exchange.getRequestBody());var reply=json.createObjectNode();
            if(status!=200)reply.put("error","private-provider-detail");
            else if(!block.isEmpty())reply.putObject("promptFeedback").put("blockReason",block);
            else {
                var candidate=reply.putArray("candidates").addObject().put("finishReason",reason);
                var parts=candidate.putObject("content").put("role","model").putArray("parts");
                parts.addObject().put("thought",true).put("text","private-thinking");parts.addObject().put("text",draft);
            }
            byte[] bytes=reply.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Location","https://example.invalid/never-follow");
            exchange.sendResponseHeaders(status,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    TeacherAssistantService client(){return new TeacherAssistantService(base,"fixture-key","fixture-model","gemini");}
    TeacherAssistantService.Request request(String type){return new TeacherAssistantService.Request("Soạn bài",""+type,1,List.of(new TeacherAssistantService.Message("user","Chủ đề gia đình"),new TeacherAssistantService.Message("assistant","Cô muốn mấy câu?")),null);}
    @Test void nativeRequestUsesSystemInstructionSchemaAndModelHistory() {
        var reply=client().generate(1L,request("SENTENCE"));assertTrue(reply.draft().contentData().contains("mẹ."));assertEquals(1,calls);
        assertTrue(received.path("systemInstruction").path("parts").path(0).path("text").asText().contains("lớp 1"));
        assertEquals("model",received.path("contents").path(1).path("role").asText());
        var text=received.path("generationConfig").path("responseFormat").path("text");assertEquals("APPLICATION_JSON",text.path("mimeType").asText());
        assertEquals(1,text.path("schema").path("properties").path("draft").path("properties").path("content").path("properties").path("sentences").path("maxItems").asInt());
    }
    @Test void writingAndQuizUseTheSameValidatedDraftContract() {
        draft="{\"reply\":\"Cô duyệt\",\"draft\":{\"title\":\"Chữ A\",\"description\":\"Tập viết\",\"contentType\":\"WRITING\",\"content\":{\"letters\":[\"A\"]}}}";
        assertEquals("WRITING",client().generate(1L,request("WRITING")).draft().contentType());
        draft="{\"reply\":\"Cô duyệt\",\"draft\":{\"title\":\"Điền chữ\",\"description\":\"Chọn đúng\",\"contentType\":\"QUIZ\",\"content\":[{\"question\":\"b□\",\"options\":[\"a\",\"o\"],\"answer\":\"a\"}]}}";
        assertEquals("QUIZ",client().generate(1L,request("QUIZ")).draft().contentType());
        draft=draft.replace("\"answer\":\"a\"","\"answer\":\"x\"");assertEquals(502,assertThrows(TeacherAssistantService.Failure.class,()->client().generate(1L,request("QUIZ"))).status);
    }
    @Test void blockedIncompleteAndEmptyCandidatesAreNotSuccessfulDrafts() {
        block="SAFETY";assertEquals(502,assertThrows(TeacherAssistantService.Failure.class,()->client().generate(1L,request("SENTENCE"))).status);
        block="";reason="MAX_TOKENS";assertEquals(502,assertThrows(TeacherAssistantService.Failure.class,()->client().generate(1L,request("SENTENCE"))).status);
        reason="STOP";draft="";assertEquals(502,assertThrows(TeacherAssistantService.Failure.class,()->client().generate(1L,request("SENTENCE"))).status);
    }
    @Test void providerErrorsAndRedirectsNeverExposeKeyOrResponseBody() {
        for(int code:List.of(400,401,403,404,429,500,302)) {
            status=code;var error=assertThrows(TeacherAssistantService.Failure.class,()->client().generate(1L,request("SENTENCE")));
            assertEquals(code==429?429:Set.of(400,401,403,404).contains(code)?503:502,error.status);
            assertFalse(error.getMessage().contains("fixture-key"));assertFalse(error.getMessage().contains("private-provider"));
        }
        assertEquals(7,calls);
    }
}
