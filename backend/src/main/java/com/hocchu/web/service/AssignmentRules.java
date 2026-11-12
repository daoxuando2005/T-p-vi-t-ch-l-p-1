package com.hocchu.web.service;

import com.fasterxml.jackson.databind.*;
import java.util.*;

/** One contract for manually authored, generated and submitted assignments. */
public final class AssignmentRules {
    private static final ObjectMapper JSON=new ObjectMapper();
    private AssignmentRules() {}
    public static boolean sameContent(String a,String b) {
        if(Objects.equals(a,b))return true;
        try{return parse(a).equals(parse(b));}catch(IllegalArgumentException e){return false;}
    }
    public static JsonNode parse(String raw) {
        try {if(raw==null||raw.length()>200_000)throw new IllegalArgumentException();var parsed=JSON.readTree(raw);if(parsed==null||parsed.isNull())throw new IllegalArgumentException();return parsed;}
        catch(Exception e){throw new IllegalArgumentException("Nội dung bài tập chưa hợp lệ.");}
    }
    private static void check(boolean value){if(!value)throw new IllegalArgumentException("Kiểm tra lại nội dung, số câu và đáp án của bài tập.");}
    private static boolean text(JsonNode n,int max){return n!=null&&n.isTextual()&&!n.asText().isBlank()&&n.asText().length()<=max;}
    public static String validate(String type,String raw) {
        check(type!=null);type=type.toUpperCase(Locale.ROOT);
        if(type.equals("WRITING")) {
            List<String> letters=letters(raw);check(!letters.isEmpty()&&letters.size()<=29&&new HashSet<>(letters).size()==letters.size());
            check(LearningTrackingService.LETTERS.containsAll(letters));return raw;
        }
        var root=parse(raw);
        if(type.equals("QUIZ")) {
            check(root.isArray()&&root.size()>=1&&root.size()<=30);
            for(var q:root) {
                check(text(q.get("question"),500)&&q.path("options").isArray()&&q.get("options").size()>=2&&q.get("options").size()<=8);
                var options=new HashSet<String>();for(var option:q.get("options")){check(text(option,150));check(options.add(option.asText()));}
                check(text(q.get("answer"),150)&&options.contains(q.get("answer").asText()));
            }
        } else if(type.equals("SENTENCE")) {
            var sentences=root.path("sentences");check(sentences.isArray()&&sentences.size()>=1&&sentences.size()<=20);
            for(var q:sentences) {
                var words=q.path("words");check(words.isArray()&&words.size()>=2&&words.size()<=12);
                for(var word:words)check(text(word,80));
                if(q.has("prompt"))check(text(q.get("prompt"),500));
            }
        } else throw new IllegalArgumentException("Chỉ hỗ trợ Tập viết chữ, Đố vui ghép chữ và Ghép câu.");
        return root.toString();
    }
    public static List<String> letters(String raw) {
        if(raw==null)throw new IllegalArgumentException("Chọn chữ cần luyện viết.");
        var result=new ArrayList<String>();
        if(raw.trim().startsWith("{")) {
            var root=parse(raw);if(root.path("letters").isArray())root.get("letters").forEach(n->result.add(n.asText().trim().toUpperCase(Locale.ROOT)));
            else result.add(root.path("letter").asText().trim().toUpperCase(Locale.ROOT));
        } else result.add(raw.trim().toUpperCase(Locale.ROOT));
        return result;
    }
    public static float scoreSentences(String content,String data) {
        var questions=parse(validate("SENTENCE",content)).get("sentences");var answers=parse(data).path("sentences");
        check(answers.isArray()&&answers.size()==questions.size());int correct=0;
        for(int i=0;i<questions.size();i++) {
            var words=questions.get(i).get("words");var answer=answers.get(i);check(answer.isArray()&&answer.size()==words.size());
            var used=new HashSet<Integer>();boolean matches=true;
            for(int n=0;n<answer.size();n++) {
                var tile=answer.get(n);check(tile.isIntegralNumber()&&tile.canConvertToInt());int index=tile.asInt();
                check(index>=0&&index<words.size()&&used.add(index));
                matches&=words.get(index).asText().equals(words.get(n).asText());
            }
            if(matches)correct++;
        }
        return Math.round(100f*correct/questions.size())/10f;
    }
}
