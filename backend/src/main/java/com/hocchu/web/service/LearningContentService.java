package com.hocchu.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hocchu.web.entity.AlphabetLesson;
import com.hocchu.web.repository.AlphabetLessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LearningContentService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AlphabetLessonRepository alphabet;
    private final ObjectMapper json=new ObjectMapper();
    @Transactional(readOnly=true)
    public List<Map<String,Object>> content() {
        Map<String,AlphabetLesson> metadata=new HashMap<>();
        alphabet.findAll().forEach(l->metadata.put(l.getLetter().toUpperCase(Locale.ROOT),l));
        var rows=jdbc.queryForList("SELECT * FROM student_learning_content");
        Map<String,Map<String,Object>> byLetter=new HashMap<>();
        for(var row:rows) byLetter.put((String)row.get("letter"),row);
        List<Map<String,Object>> result=new ArrayList<>();
        for(String letter:LearningTrackingService.LETTERS) {
            var row=byLetter.get(letter);
            if(row==null) throw new IllegalStateException("Missing learning content");
            var lesson=metadata.get(letter);var item=new LinkedHashMap<String,Object>();
            item.put("letter",letter);
            item.put("sound",lesson!=null?value(lesson.getPronunciationGuide(),row.get("sound")):row.get("sound"));
            item.put("word",lesson!=null?value(lesson.getExampleWord(),row.get("example_word")):row.get("example_word"));
            item.put("picture",row.get("picture"));item.put("hidden",lesson!=null&&Boolean.TRUE.equals(lesson.getIsHidden()));
            item.put("imageUrl",lesson==null?"":asset(lesson.getImageUrl(),"example_images"));
            item.put("audioUrl",lesson==null?"":asset(lesson.getAudioUrl(),"audio"));
            item.put("exampleAudioUrl",lesson==null?"":asset(lesson.getExampleAudioUrl(),"audio"));
            try {item.put("practices",json.readTree(row.get("practices").toString()));}
            catch(Exception e) {throw new IllegalStateException("Invalid learning content",e);}
            result.add(item);
        }
        return result;
    }
    private static Object value(String value,Object fallback) {return value==null||value.isBlank()?fallback:value;}
    private static String asset(String raw,String folder) {
        if(raw==null||raw.isBlank())return "";
        String value=raw.trim();
        if(!value.contains(":" )&&!value.contains("/"))value="/"+folder+"/"+value;
        return MediaStorageService.publicUrl(value);
    }
}
