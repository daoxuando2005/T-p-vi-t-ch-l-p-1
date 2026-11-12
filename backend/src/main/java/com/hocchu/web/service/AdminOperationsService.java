package com.hocchu.web.service;

import com.fasterxml.jackson.databind.*;
import com.hocchu.web.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class AdminOperationsService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TeacherAssistantService assistant;
    @Autowired private LearningContentService content;
    private final ObjectMapper json=new ObjectMapper();
    private long count(String sql,Object...args){return jdbc.queryForObject(sql,Long.class,args);}
    @Transactional(readOnly=true)
    public Map<String,Object> overview() {
        var data=new LinkedHashMap<String,Object>();
        data.put("totalUsers",count("SELECT COUNT(*) FROM users"));
        data.put("lockedUsers",count("SELECT COUNT(*) FROM users WHERE enabled=FALSE OR enabled IS NULL"));
        data.put("totalClasses",count("SELECT COUNT(*) FROM classes"));
        data.put("totalAssignments",count("SELECT COUNT(*) FROM assignments"));
        data.put("totalSubmissions",count("SELECT COUNT(*) FROM submissions"));
        data.put("approvedLinks",count("SELECT COUNT(*) FROM parent_student_links WHERE status='APPROVED'"));
        data.put("hiddenLetters",count("SELECT COUNT(*) FROM alphabet_lessons WHERE is_hidden=TRUE"));
        for(String role:List.of("STUDENT","TEACHER","PARENT","ADMIN")) data.put(role.toLowerCase(Locale.ROOT)+"Count",
            count("SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name=?","ROLE_"+role));
        LocalDate today=LocalDate.now(LearningTrackingService.SCHOOL_ZONE);
        var days=new ArrayList<Map<String,Object>>();
        for(int i=6;i>=0;i--){LocalDate day=today.minusDays(i);var start=day.atStartOfDay(LearningTrackingService.SCHOOL_ZONE).toInstant();var end=day.plusDays(1).atStartOfDay(LearningTrackingService.SCHOOL_ZONE).toInstant();
            var row=new LinkedHashMap<String,Object>();row.put("date",day.toString());
            row.put("practices",count("SELECT COUNT(*) FROM learning_events WHERE occurred_at_utc>=? AND occurred_at_utc<? AND kind<>'LESSON_COMPLETE'",Timestamp.from(start),Timestamp.from(end)));
            row.put("activeStudents",count("SELECT COUNT(DISTINCT student_id) FROM learning_events WHERE occurred_at_utc>=? AND occurred_at_utc<?",Timestamp.from(start),Timestamp.from(end)));
            row.put("submissions",count("SELECT COUNT(*) FROM submissions WHERE submitted_at>=? AND submitted_at<?",day.atStartOfDay(),day.plusDays(1).atStartOfDay()));days.add(row);
        }
        data.put("days",days);data.put("schoolDate",today.toString());data.put("updatedAt",Instant.now().toString());return data;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> classes() {
        return jdbc.queryForList("SELECT c.id,c.name,c.description,c.invite_code AS inviteCode,c.created_at AS createdAt,u.id AS teacherId,u.full_name AS teacherName,u.username AS teacherUsername,(SELECT COUNT(*) FROM class_members m WHERE m.class_id=c.id) AS studentCount,(SELECT COUNT(*) FROM assignments a WHERE a.class_id=c.id) AS assignmentCount,(SELECT COUNT(*) FROM submissions s JOIN assignments a ON a.id=s.assignment_id WHERE a.class_id=c.id) AS submissionCount FROM classes c JOIN users u ON u.id=c.teacher_id ORDER BY c.id DESC");
    }
    @Transactional(readOnly=true)
    public Map<String,Object> classDetail(long id) {
        var rows=jdbc.queryForList("SELECT c.id,c.name,c.description,u.full_name AS teacherName,u.username AS teacherUsername FROM classes c JOIN users u ON u.id=c.teacher_id WHERE c.id=?",id);
        if(rows.isEmpty())throw new IllegalArgumentException("Không tìm thấy lớp.");
        var data=new LinkedHashMap<>(rows.get(0));
        data.put("students",jdbc.queryForList("SELECT u.id,u.username,u.full_name AS fullName,u.enabled,(SELECT COUNT(*) FROM submissions s JOIN assignments a ON a.id=s.assignment_id WHERE s.student_id=u.id AND a.class_id=?) AS submittedCount FROM class_members m JOIN users u ON u.id=m.student_id WHERE m.class_id=? ORDER BY u.full_name,u.id",id,id));
        data.put("assignments",jdbc.queryForList("SELECT a.id,a.title,a.content_type AS contentType,a.due_date AS dueDate,(SELECT COUNT(*) FROM submissions s WHERE s.assignment_id=a.id) AS submissionCount FROM assignments a WHERE a.class_id=? ORDER BY a.id DESC",id));return data;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> familyLinks() {
        return jdbc.queryForList("SELECT l.id,l.status,l.linked_at AS linkedAt,p.id AS parentId,p.username AS parentUsername,p.full_name AS parentName,s.id AS studentId,s.username AS studentUsername,s.full_name AS studentName FROM parent_student_links l JOIN users p ON p.id=l.parent_id JOIN users s ON s.id=l.student_id ORDER BY l.id DESC");
    }
    @Transactional
    public ApiResponse unlink(long id) {
        var rows=jdbc.queryForList("SELECT student_id FROM parent_student_links WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty())throw new IllegalArgumentException("Liên kết không còn tồn tại. Hãy tải lại danh sách.");
        jdbc.update("DELETE FROM parent_student_links WHERE id=?",id);
        jdbc.update("DELETE FROM parent_link_invites WHERE student_id=?",rows.get(0).get("student_id"));
        return new ApiResponse(true,"Đã gỡ liên kết và mã mời đang chờ. Giữ nguyên tiến độ/bài nộp; liên kết lại bằng mã mới từ học sinh.");
    }
    public Map<String,Object> settings() {
        return Map.of("backend","UP","serverTime",Instant.now().toString(),"aiConfigured",assistant.status().get("configured"),
            "aiMessage","Khóa và model được cấu hình riêng ở backend; không trả khóa về trình duyệt.","chatEnabled",false,"alphabetSize",29);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> learningContent(){return content.content();}
    public record Practice(String text,String picture,List<String> sentence) {}
    public record Curriculum(List<Practice> practices) {}
    public static void validateCurriculum(String letter,Curriculum value) {
        if(!LearningTrackingService.LETTERS.contains(letter)||value==null||value.practices()==null||value.practices().size()!=3)
            throw new IllegalArgumentException("Mỗi chữ cần đúng ba cặp từ và câu luyện tập.");
        for(var p:value.practices()) {
            // Android replaces an exact lowercase character, preserving every other tone mark.
            if(p==null||p.text()==null||p.text().isBlank()||p.text().length()>100||!p.text().contains(letter.toLowerCase(Locale.ROOT))
                ||p.picture()==null||p.picture().isBlank()||p.picture().length()>40||p.sentence()==null||p.sentence().size()<2||p.sentence().size()>12
                ||p.sentence().stream().anyMatch(w->w==null||w.isBlank()||w.length()>80))
                throw new IllegalArgumentException("Từ phải có chữ đang học viết thường; cần minh họa và câu có 2–12 thẻ, tối đa 80 ký tự/thẻ.");
        }
    }
    @Transactional
    public ApiResponse saveCurriculum(String letter,Curriculum value) {
        validateCurriculum(letter,value);
        try {int updated=jdbc.update("UPDATE student_learning_content SET practices=?,updated_at_utc=? WHERE letter=?",json.writeValueAsString(value.practices()),Timestamp.from(Instant.now()),letter);
            if(updated!=1)throw new IllegalArgumentException("Không tìm thấy nội dung cho chữ này.");
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("Nội dung luyện tập chưa hợp lệ.");}
        return new ApiResponse(true,"Đã lưu ba cặp từ/câu. App nhận nội dung khi cập nhật học liệu; tiến độ đã đạt được giữ nguyên.");
    }
}
