package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentService {

    @Autowired
    private AlphabetLessonRepository alphabetLessonRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private ClassMemberRepository classMemberRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private HandwritingAssessment handwriting;

    public List<AlphabetLesson> getAlphabetLessons() {
        return alphabetLessonRepository.findAll();
    }

    @Transactional
    public ApiResponse joinClass(Long studentId, String inviteCode) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return new ApiResponse(false, "Vui lòng nhập mã mời lớp học!");
        }

        String cleanCode = inviteCode.trim();
        Optional<Clazz> selectedClass = clazzRepository.findByInviteCode(cleanCode)
                .or(() -> clazzRepository.findByInviteCode(cleanCode.toUpperCase()))
                .or(() -> clazzRepository.findByInviteCode(cleanCode.toLowerCase()));
        if (selectedClass.isEmpty()) return new ApiResponse(false, "Không tìm thấy lớp học. Bé kiểm tra lại mã từ cô giáo nhé.");
        Clazz clazz = selectedClass.get();

        if (classMemberRepository.existsByClazzIdAndStudentId(clazz.getId(), studentId)) {
            return new ApiResponse(false, "Bé đã tham gia lớp học này rồi.");
        }

        ClassMember member = ClassMember.builder()
                .clazz(clazz)
                .student(student)
                .build();

        classMemberRepository.save(member);

        // Tạo thông báo hệ thống
        Notification notif = Notification.builder()
                .user(student)
                .title("Đã tham gia lớp học")
                .content("Bé đã tham gia lớp '" + clazz.getName() + "' thành công!")
                .type("SYSTEM")
                .isRead(false)
                .build();
        notificationRepository.save(notif);

        // Gửi thông báo cho Giáo viên
        Notification teacherNotif = Notification.builder()
                .user(clazz.getTeacher())
                .title("Học sinh mới tham gia lớp")
                .content("Học sinh " + student.getFullName() + " đã tham gia lớp '" + clazz.getName() + "' của bạn.")
                .type("SYSTEM")
                .isRead(false)
                .build();
        notificationRepository.save(teacherNotif);

        return new ApiResponse(true, "Tham gia lớp học '" + clazz.getName() + "' thành công!");
    }

    @Transactional
    public ApiResponse leaveClass(Long studentId, Long classId) {
        ClassMember member = classMemberRepository.findByClazzIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Bé không ở trong lớp học này"));

        classMemberRepository.delete(member);
        return new ApiResponse(true, "Đã rời khỏi lớp học thành công");
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getJoinedClasses(Long studentId) {
        return classMemberRepository.findByStudentId(studentId).stream()
                .filter(cm -> cm.getClazz() != null)
                .map(cm -> {
                    Clazz c = cm.getClazz();
                    Long teacherId = (c.getTeacher() != null) ? c.getTeacher().getId() : null;
                    String teacherName = (c.getTeacher() != null) ? c.getTeacher().getFullName() : "Giáo viên";
                    return ClassResponse.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .description(c.getDescription())
                            .inviteCode(c.getInviteCode())
                            .teacherId(teacherId)
                            .teacherName(teacherName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(Long studentId) {
        // Lấy danh sách lớp đã tham gia
        List<ClassMember> memberships = classMemberRepository.findByStudentId(studentId);
        List<AssignmentResponse> result = new ArrayList<>();

        for (ClassMember cm : memberships) {
            Clazz clazz = cm.getClazz();
            if (clazz == null) continue;
            List<Assignment> assignments = assignmentRepository.findByClazzId(clazz.getId());
            for (Assignment asm : assignments) {
                // Kiểm tra xem đã nộp bài chưa (lấy bản ghi mới nhất, an toàn tuyệt đối với bản ghi trùng)
                Optional<Submission> subOpt = submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(asm.getId(), studentId);
                LocalDateTime itemCreated = asm.getCreatedAt() != null ? asm.getCreatedAt() : (asm.getDueDate() != null ? asm.getDueDate() : LocalDateTime.now());
                
                result.add(AssignmentResponse.builder()
                        .id(asm.getId())
                        .title(asm.getTitle())
                        .description(asm.getDescription())
                        .contentType(asm.getContentType())
                        .contentData(asm.getContentData())
                        .dueDate(asm.getDueDate())
                        .classId(clazz.getId())
                        .className(clazz.getName())
                        .createdAt(itemCreated)
                        .isSubmitted(subOpt.isPresent())
                        .score(subOpt.map(Submission::getScore).orElse(null))
                        .feedback(subOpt.map(Submission::getFeedback).orElse(null))
                        .assignedBy(asm.getAssignedBy())
                        .build());
            }
        }
        
        // Sắp xếp bài tập mới nhất lên đầu (null-safe)
        result.sort((a, b) -> {
            LocalDateTime tA = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
            LocalDateTime tB = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
            return tB.compareTo(tA);
        });
        return result;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ApiResponse submitAssignment(Long studentId, Long assignmentId, SubmissionRequest request) {
        // Lock before reading a receipt. READ_COMMITTED also avoids a stale MySQL
        // repeatable-read snapshot when a concurrent request just committed.
        Assignment assignment = assignmentRepository.findForSubmissionById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        if (assignment.getClazz() == null || !classMemberRepository.existsByClazzIdAndStudentId(
                assignment.getClazz().getId(), studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Học sinh chưa tham gia lớp này");
        }
        if (request.getSubmissionData() == null || request.getSubmissionData().length() > 4_000_000) {
            return new ApiResponse(false, "Dữ liệu bài nộp không hợp lệ hoặc quá lớn.");
        }

        var existing=submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(assignmentId,studentId);
        if(existing.isPresent()) {
            if(Objects.equals(existing.get().getSubmissionData(),request.getSubmissionData()))
                return new ApiResponse(true,"Bài đã được tiếp nhận trước đó. Điểm và nhận xét của cô được giữ nguyên.");
            return new ApiResponse(false,"Bài đã nộp, không thể thay đổi nội dung.");
        }
        Float score = null;
        String assessmentData=null;
        String feedback = "Đã nhận bài viết. Chờ giáo viên chấm và nhận xét.";
        if ("QUIZ".equalsIgnoreCase(assignment.getContentType())) {
            try {
                score = calculateQuizScore(assignment.getContentData(), request.getSubmissionData());
            } catch (IllegalArgumentException e) {
                return new ApiResponse(false, "Câu trả lời hoặc nội dung bài tập không hợp lệ.");
            }
            feedback = "Đã tự động chấm điểm trắc nghiệm: " + score + "/10";
        } else if ("SENTENCE".equalsIgnoreCase(assignment.getContentType())) {
            try {score=AssignmentRules.scoreSentences(assignment.getContentData(),request.getSubmissionData());}
            catch(IllegalArgumentException e){return new ApiResponse(false,"Các thẻ từ của bài ghép câu chưa hợp lệ.");}
            feedback="Đã tự động chấm bài ghép câu: "+score+"/10";
        } else if ("WRITING".equalsIgnoreCase(assignment.getContentType())) {
            if (!validWritingSubmission(assignment.getContentData(),request.getSubmissionData())) {
                return new ApiResponse(false, "Bài viết phải có ảnh PNG hoặc JPEG hợp lệ.");
            }
            try {
                var images=request.getSubmissionData().startsWith("data:image/")
                    ?objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("letter",AssignmentRules.letters(assignment.getContentData()).get(0)).put("image",request.getSubmissionData()))
                    :objectMapper.readTree(request.getSubmissionData());
                var ratings=objectMapper.createArrayNode();float total=0;
                for(var image:images) {
                    String source=image.get("image").asText();byte[] bytes=Base64.getDecoder().decode(source.substring(source.indexOf(',')+1));
                    var rating=handwriting.assess(image.get("letter").asText(),javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes)));
                    ratings.addObject().put("letter",image.get("letter").asText()).put("score",rating.score())
                        .put("coverage",rating.coverage()).put("precision",rating.precision()).put("missingParts",rating.missingParts()).put("feedback",rating.feedback());total+=rating.score();
                }
                score=Math.round(total/images.size())/10f;assessmentData=ratings.toString();
                feedback="Điểm bám mẫu tự động: "+score+"/10. Cô có thể xem lại và điều chỉnh; chưa đánh giá thứ tự nét.";
            }catch(Exception e){throw new IllegalArgumentException("Chưa đánh giá được ảnh viết. Bé kiểm tra lại ảnh bài làm.");}
        } else {
            return new ApiResponse(false, "Loại bài tập chưa được hỗ trợ.");
        }
        final Float finalScore = score;

        Submission submission=Submission.builder().assignment(assignment).student(student)
            .submissionData(request.getSubmissionData()).score(score).autoScore(score).gradeSource("AUTO").assessmentData(assessmentData).feedback(feedback)
            .submittedAt(LocalDateTime.now(LearningTrackingService.SCHOOL_ZONE)).build();

        submissionRepository.save(submission);

        // Thông báo phụ huynh
        parentStudentLinkRepository.findAllByStudentId(studentId).forEach(link -> {
            if ("APPROVED".equals(link.getStatus())) {
                Notification parentNotif = Notification.builder()
                        .user(link.getParent())
                        .title("Bé " + student.getFullName() + " vừa nộp bài tập")
                        .content("Con vừa nộp bài tập '" + assignment.getTitle() + "' trong lớp '" + assignment.getClazz().getName() + "'. "
                                + (finalScore == null ? "Chờ giáo viên chấm bài." : "Điểm bài: " + finalScore))
                        .type("ASSIGNMENT")
                        .isRead(false)
                        .build();
                notificationRepository.save(parentNotif);
            }
        });

        // Thông báo Giáo viên
        Notification teacherNotif = Notification.builder()
                .user(assignment.getClazz().getTeacher())
                .title("Bài nộp mới trong lớp " + assignment.getClazz().getName())
                .content("Học sinh " + student.getFullName() + " vừa nộp bài '" + assignment.getTitle() + "'.")
                .type("ASSIGNMENT")
                .isRead(false)
                .build();
        notificationRepository.save(teacherNotif);

        return new ApiResponse(true, score == null ? "Đã nộp bài viết. Chờ giáo viên chấm bài." : "Nộp bài tập thành công! Điểm số: " + score);
    }

    private float calculateQuizScore(String assignmentJson, String submissionJson) {
        try {
            var questions = objectMapper.readTree(assignmentJson);
            var answers = objectMapper.readTree(submissionJson).get("answers");
            if (!questions.isArray() || questions.isEmpty() || answers == null || !answers.isArray()
                    || answers.size() != questions.size()) throw new IllegalArgumentException();
            int correct = 0;
            for (int i = 0; i < questions.size(); i++) {
                var q = questions.get(i);
                var answer = answers.get(i);
                if (!answer.isTextual() || !q.path("answer").isTextual()
                        || !q.path("options").isArray()) throw new IllegalArgumentException();
                boolean option = false;
                for (var value : q.get("options")) {
                    if (value.isTextual() && value.asText().equals(answer.asText())) option = true;
                }
                if (!option) throw new IllegalArgumentException();
                if (q.get("answer").asText().trim().equalsIgnoreCase(answer.asText().trim())) correct++;
            }
            return Math.round(100f * correct / questions.size()) / 10f;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid quiz submission");
        }
    }

    private boolean validWritingSubmission(String content,String data) {
        try {
            Set<String> expected=new LinkedHashSet<>();
            String raw=content.trim();
            if(raw.startsWith("{")) {
                var target=objectMapper.readTree(raw);
                if(target.path("letters").isArray())for(var letter:target.get("letters"))expected.add(letter.asText().trim().toUpperCase(Locale.ROOT));
                else expected.add(target.path("letter").asText().trim().toUpperCase(Locale.ROOT));
            } else expected.add(raw.toUpperCase(Locale.ROOT));
            if(expected.isEmpty()||!LearningTrackingService.LETTERS.containsAll(expected))return false;
            if(data.startsWith("data:image/"))return expected.size()==1&&validWritingImage(data);
            var images=objectMapper.readTree(data);
            if(!images.isArray()||images.size()!=expected.size())return false;
            Set<String> actual=new HashSet<>();
            for(var item:images) {
                if(!actual.add(item.path("letter").asText().trim().toUpperCase(Locale.ROOT))||!validWritingImage(item.path("image").asText()))return false;
            }
            return actual.equals(expected);
        }catch(Exception e){return false;}
    }

    private boolean validWritingImage(String image) {
        boolean png = image.startsWith("data:image/png;base64,");
        boolean jpeg = image.startsWith("data:image/jpeg;base64,");
        if (!png && !jpeg) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(image.substring(image.indexOf(',') + 1));
            boolean signature=png?bytes.length>=8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71
                :bytes.length>=3&&bytes[0]==(byte)255&&bytes[1]==(byte)216&&bytes[2]==(byte)255;
            if(!signature)return false;
            try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers=javax.imageio.ImageIO.getImageReaders(input);if(!readers.hasNext())return false;
                var reader=readers.next();try{reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<=0||h<=0||w>4096||h>4096||(long)w*h>8_000_000)return false;
                    return reader.read(0)!=null;
                }finally{reader.dispose();}
            }
        } catch (Exception e) { return false; }
    }

    @Autowired
    private LearningTrackingService tracking;

    @Transactional
    public Map<String, String> getParentLinkCode(Long studentId) {
        return tracking.issueLinkCode(studentId);
    }
}
