package com.hocchu.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static com.hocchu.web.service.TeacherAssistantService.Failure;

/** Native Gemini HTTP adapter. Secrets stay in headers; output still needs AssignmentRules validation. */
final class GeminiDraftClient {
    private final String base,key,model;
    private final ObjectMapper json=new ObjectMapper();
    GeminiDraftClient(String base,String key,String model){this.base=base;this.key=key;this.model=model;}

    JsonNode generate(JsonNode messages,String type,int count) throws Exception {
        if(!model.matches("[A-Za-z0-9._-]+"))throw new Failure(503,"Tên model Gemini trên máy chủ chưa hợp lệ.");
        URI uri=URI.create(base+"/models/"+model+":generateContent");
        boolean local=Set.of("localhost","127.0.0.1","::1").contains(uri.getHost());
        if(uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!("https".equals(uri.getScheme())||local&&"http".equals(uri.getScheme())))
            throw new Failure(503,"Địa chỉ Gemini phải dùng HTTPS (HTTP chỉ dành cho kiểm thử local).");
        var body=json.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text",messages.get(0).path("content").asText());
        var contents=body.putArray("contents");
        for(int i=1;i<messages.size();i++) {
            var item=messages.get(i);var turn=contents.addObject().put("role",item.path("role").asText().equals("assistant")?"model":"user");
            turn.putArray("parts").addObject().put("text",item.path("content").asText());
        }
        var config=body.putObject("generationConfig").put("maxOutputTokens",8192);
        config.putObject("responseFormat").putObject("text").put("mimeType","APPLICATION_JSON").set("schema",schema(type,count));
        var connection=(HttpURLConnection)uri.toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(30000);
            connection.setRequestMethod("POST");connection.setRequestProperty("x-goog-api-key",key);
            connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setDoOutput(true);
            try(var out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int status=connection.getResponseCode();
            if(status==400||status==401||status==403||status==404)throw new Failure(503,"Khóa, model hoặc cấu hình Gemini chưa hợp lệ. Vui lòng kiểm tra trên máy chủ.");
            if(status==429)throw new Failure(429,"Gemini đang giới hạn lượt dùng hoặc chưa có hạn mức. Cô thử lại sau.");
            if(status!=200)throw new Failure(502,"Gemini chưa đáp ứng. Cô có thể thử lại hoặc soạn thủ công.");
            JsonNode envelope;
            try(var in=connection.getInputStream()) {
                byte[] bytes=in.readNBytes(131073);
                if(bytes.length>131072)throw new Failure(502,"Bản nháp Gemini quá dài.");
                envelope=json.readTree(bytes);
            }
            if(envelope.path("promptFeedback").hasNonNull("blockReason"))throw new Failure(502,"Gemini chưa thể soạn yêu cầu này. Cô thử mô tả lại bài học.");
            var candidate=envelope.path("candidates").path(0);
            if(!candidate.path("finishReason").asText().equals("STOP"))throw new Failure(502,"Gemini chưa trả đủ nội dung. Cô thử yêu cầu ít câu hơn hoặc đổi mô tả.");
            var text=new StringBuilder();
            for(var part:candidate.path("content").path("parts"))if(!part.path("thought").asBoolean(false)&&part.path("text").isTextual())text.append(part.path("text").asText());
            if(text.isEmpty())throw new Failure(502,"Gemini chưa trả bản nháp. Cô vui lòng thử lại.");
            return json.readTree(text.toString());
        }catch(SocketTimeoutException e) {
            throw new Failure(504,"Gemini phản hồi quá lâu. Cô đợi một chút rồi thử lại hoặc soạn bài thủ công.");
        }catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new Failure(502,"Bản nháp Gemini chưa đúng định dạng. Cô vui lòng thử lại.");
        }catch(java.io.IOException e) {
            throw new Failure(502,"Chưa kết nối được Gemini. Vui lòng kiểm tra mạng máy chủ và thử lại.");
        }finally{connection.disconnect();}
    }
    private ObjectNode string(){return json.createObjectNode().put("type","string");}
    private ObjectNode object(Map<String,? extends JsonNode> fields) {
        var value=json.createObjectNode().put("type","object");var properties=value.putObject("properties");var required=value.putArray("required");
        fields.forEach((name,schema)->{properties.set(name,schema);required.add(name);});return value;
    }
    private ObjectNode array(JsonNode items,int min,int max) {
        var value=json.createObjectNode().put("type","array").put("minItems",min).put("maxItems",max);value.set("items",items);return value;
    }
    private JsonNode schema(String type,int count) {
        JsonNode content=switch(type) {
            case "WRITING" -> object(Map.of("letters",array(string(),count,count)));
            case "QUIZ" -> array(object(Map.of("question",string(),"options",array(string(),2,6),"answer",string())),count,count);
            case "SENTENCE" -> object(Map.of("sentences",array(object(Map.of("prompt",string(),"words",array(string(),2,12))),count,count)));
            default -> throw new IllegalArgumentException("Loại bài chưa hợp lệ.");
        };
        var contentType=string();contentType.putArray("enum").add(type);
        return object(Map.of("reply",string(),"draft",object(Map.of("title",string(),"description",string(),"contentType",contentType,"content",content))));
    }
}
