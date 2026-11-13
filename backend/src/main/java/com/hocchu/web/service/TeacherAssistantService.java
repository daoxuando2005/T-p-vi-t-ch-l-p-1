package com.hocchu.web.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Draft generation over ordinary HTTP. Never sends rosters or publishes assignments. */
@Service
public class TeacherAssistantService {
    private final String base,key,model,provider;
    private final ObjectMapper json=new ObjectMapper();
    private final Set<Long> busy=ConcurrentHashMap.newKeySet();
    private final Map<Long,ArrayDeque<Long>> calls=new ConcurrentHashMap<>();
    public record Message(String role,String content) {}
    public record Request(String message,String type,int count,List<Message> history,Draft currentDraft) {}
    public record Draft(String title,String description,String contentType,String contentData) {}
    public record Reply(String reply,Draft draft) {}
    public static class Failure extends RuntimeException {
        public final int status;public Failure(int status,String message){super(message);this.status=status;}
    }
    @Autowired
    public TeacherAssistantService(@Value("${app.ai.base-url}") String base,
        @Value("${app.ai.api-key:}") String key,@Value("${app.ai.model:}") String model,
        @Value("${app.ai.provider:gemini}") String provider) {
        this.base=base.replaceAll("/+$","");this.key=key.trim();this.model=model.trim();this.provider=provider.trim();
    }
    public TeacherAssistantService(String base,String key,String model) {
        this(base,key,model,"openai-compatible");
    }
    public Map<String,Object> status() {
        return Map.of("configured",configured(),"provider",provider,"message",configured()?"Đã có cấu hình AI. Gửi yêu cầu để kiểm tra kết nối và soạn bản nháp.":"Chưa cấu hình dịch vụ AI trên máy chủ. Cô vẫn có thể soạn bài thủ công hoặc dùng kho mẫu.");
    }
    private boolean configured(){return Set.of("gemini","openai-compatible").contains(provider)&&!key.isBlank()&&!model.isBlank();}
    public Reply generate(Long teacherId,Request request) {
        if(!configured())throw new Failure(503,"Chưa cấu hình nhà cung cấp, khóa và model AI trên máy chủ.");
        validate(request);
        if(!busy.add(teacherId))throw new Failure(429,"Trợ lý đang soạn bài trước. Cô đợi một chút nhé.");
        try {
            var history=calls.computeIfAbsent(teacherId,id->new ArrayDeque<>());
            synchronized(history){long now=System.currentTimeMillis();while(!history.isEmpty()&&history.peekFirst()<now-60000)history.removeFirst();
                if(history.size()>=10)throw new Failure(429,"Cô đã gửi nhiều yêu cầu. Vui lòng thử lại sau một phút.");history.addLast(now);}
            var body=json.createObjectNode().put("model",model).put("max_completion_tokens",3500);
            body.putObject("response_format").put("type","json_object");
            var messages=body.putArray("messages");messages.addObject().put("role","system").put("content",PROMPT);
            if(request.currentDraft()!=null)messages.addObject().put("role","user").put("content","Bản nháp hiện tại để sửa theo yêu cầu mới: "+json.writeValueAsString(request.currentDraft()));
            if(request.history()!=null)for(var item:request.history())messages.addObject().put("role",item.role()).put("content",item.content());
            messages.addObject().put("role","user").put("content","Loại bài: "+request.type()+". Số chữ/câu: "+request.count()+". Yêu cầu: "+request.message());
            JsonNode response;
            if(provider.equals("gemini")) response=new GeminiDraftClient(base,key,model).generate(messages,request.type(),request.count());
            else {
                var envelope=json.readTree(exchange(body.toString()));var choice=envelope.path("choices").path(0);
                if(!choice.path("finish_reason").asText().equals("stop"))throw new Failure(502,"AI chưa trả đủ nội dung. Cô thử yêu cầu ít câu hơn.");
                response=json.readTree(choice.path("message").path("content").asText());
            }
            String title=response.path("draft").path("title").asText(),description=response.path("draft").path("description").asText();
            String type=response.path("draft").path("contentType").asText(),reply=response.path("reply").asText();
            if(!type.equals(request.type())||title.isBlank()||title.length()>160||description.length()>2000||reply.isBlank()||reply.length()>2000)
                throw new Failure(502,"Bản nháp AI chưa hợp lệ. Cô thử mô tả cụ thể hơn.");
            JsonNode generated=response.path("draft").path("content").deepCopy();
            // Model-generated links are not trusted uploads. Cô adds media explicitly in the editor.
            if(type.equals("QUIZ")&&generated.isArray())for(var question:generated)if(question.isObject())
                ((com.fasterxml.jackson.databind.node.ObjectNode)question).remove(List.of("imageUrl","audioUrl"));
            String content=AssignmentRules.validate(type,generated.toString());
            int amount=type.equals("WRITING")?AssignmentRules.letters(content).size():type.equals("QUIZ")?AssignmentRules.parse(content).size():AssignmentRules.parse(content).get("sentences").size();
            if(amount!=request.count())throw new Failure(502,"AI trả số câu/chữ chưa đúng yêu cầu. Cô vui lòng thử lại.");
            return new Reply(reply,new Draft(title,description,type,content));
        }catch(Failure e){throw e;}
        catch(Exception e){throw new Failure(502,"Chưa nhận được bản nháp hợp lệ từ AI. Cô thử lại hoặc soạn thủ công.");}
        finally{busy.remove(teacherId);}
    }
    private static void validate(Request r) {
        if(r==null||r.message()==null||r.message().isBlank()||r.message().length()>2000||r.type()==null||!Set.of("WRITING","QUIZ","SENTENCE").contains(r.type())||r.count()<1||r.count()>20||r.history()!=null&&r.history().size()>6)
            throw new Failure(400,"Nhập yêu cầu không quá 2000 ký tự và chọn từ 1 đến 20 câu/chữ.");
        if(r.currentDraft()!=null) {
            var draft=r.currentDraft();if(draft.title()==null||draft.title().length()>160||draft.description()==null||draft.description().length()>2000)throw new Failure(400,"Bản nháp hiện tại chưa hợp lệ.");
            try{AssignmentRules.validate(draft.contentType(),draft.contentData());}catch(IllegalArgumentException e){throw new Failure(400,"Bản nháp hiện tại chưa hợp lệ.");}
        }
        if(r.history()!=null)for(var m:r.history())if(m==null||m.role()==null||!Set.of("user","assistant").contains(m.role())||m.content()==null||m.content().length()>2500)
            throw new Failure(400,"Lịch sử trao đổi chưa hợp lệ.");
    }
    private String exchange(String body) throws Exception {
        URI uri=URI.create(base+"/chat/completions");boolean local=Set.of("localhost","127.0.0.1","::1").contains(uri.getHost());
        if(uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getScheme().equals("https")||local&&uri.getScheme().equals("http")))
            throw new Failure(503,"Địa chỉ dịch vụ AI phải dùng HTTPS (HTTP chỉ cho máy chủ local).");
        var connection=(HttpURLConnection)uri.toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(8000);connection.setReadTimeout(30000);connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization","Bearer "+key);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setDoOutput(true);
            try(var out=connection.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
            int status=connection.getResponseCode();
            if(status==401||status==403)throw new Failure(503,"Khóa hoặc quyền truy cập AI trên máy chủ chưa hợp lệ.");
            if(status==429)throw new Failure(429,"Dịch vụ AI đang giới hạn lượt dùng hoặc chưa có hạn mức. Cô thử lại sau.");
            if(status!=200)throw new Failure(502,"Dịch vụ AI chưa đáp ứng. Cô có thể soạn bài thủ công.");
            try(var in=connection.getInputStream()){byte[] bytes=in.readNBytes(131073);if(bytes.length>131072)throw new Failure(502,"Bản nháp AI quá dài.");return new String(bytes,StandardCharsets.UTF_8);}
        }finally{connection.disconnect();}
    }
    private static final String PROMPT="""
        Bạn là trợ lý soạn bài tiếng Việt lớp 1 cho giáo viên. Chỉ tạo bản nháp để cô duyệt, không giao bài và không hỏi thông tin riêng của trẻ.
        Nội dung ngắn, có dấu chính xác, phù hợp trẻ lớp 1. Đúng loại và số lượng cô yêu cầu. Trả JSON thuần:
        {"reply":"Giải thích ngắn bằng tiếng Việt", "draft":{"title":"Tiêu đề", "description":"Hướng dẫn", "contentType":"WRITING|QUIZ|SENTENCE", "content":...}}.
        WRITING content là {"letters":["A","Ă"]}, chỉ dùng bảng 29 chữ A Ă Â B C D Đ E Ê G H I K L M N O Ô Ơ P Q R S T U Ư V X Y, chữ không trùng.
        QUIZ content là mảng [{"question":"Điền chữ còn thiếu: c□", "options":["a","o","u"], "answer":"a"}], đáp án phải có trong lựa chọn. Dùng đố vui điền/ghép chữ thành từ có nghĩa.
        SENTENCE content là {"sentences":[{"prompt":"Ghép câu đúng", "words":["Bé","yêu","mẹ."]}]}, mỗi câu có 2 đến 12 thẻ từ/cụm từ theo thứ tự đáp án; không tự đảo thứ tự đáp án trong content.
        Chỉ trả một bản nháp cho loại đang yêu cầu. Không thêm HTML, URL, markdown, script hay trường không liên quan.
        """;
}
