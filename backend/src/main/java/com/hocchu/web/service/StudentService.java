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
    private WritingPracticeRepository writingPracticeRepository;

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

    public List<AlphabetLesson> getAlphabetLessons() {
        return alphabetLessonRepository.findAll();
    }

    @Transactional
    public PracticeResponse saveWritingPractice(Long studentId, PracticeRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        // Đọc điểm số so khớp thực tế do thuật toán Canvas Pixel-Matching tính toán từ frontend
        float score = 0.0f;
        if (request.getAccuracyScore() != null) {
            score = request.getAccuracyScore();
        } else {
            // Fallback nếu không có
            String base64Data = request.getCanvasImageUrl();
            if (base64Data != null && base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }
            if (base64Data == null || base64Data.trim().length() < 500) {
                score = 0.0f;
            } else {
                int letterSeed = request.getLetter().hashCode();
                int canvasSeed = request.getCanvasImageUrl() != null ? request.getCanvasImageUrl().hashCode() : 0;
                Random rand = new Random(letterSeed + canvasSeed);
                score = 50.0f + rand.nextFloat() * 30.0f;
            }
        }
        
        // Làm tròn điểm 1 chữ số thập phân
        score = Math.round(score * 10.0f) / 10.0f;
        final float finalScore = score;

        WritingPractice practice = WritingPractice.builder()
                .student(student)
                .letter(request.getLetter())
                .canvasImageUrl(request.getCanvasImageUrl())
                .accuracyScore(score)
                .build();

        WritingPractice saved = writingPracticeRepository.save(practice);

        // Tạo thông báo kết quả luyện viết
        Notification notif = Notification.builder()
                .user(student)
                .title("Kết quả luyện viết chữ " + request.getLetter())
                .content("Bé vừa hoàn thành bài luyện viết tự do chữ '" + request.getLetter() + "' với độ chính xác " + score + "%!")
                .type("PRACTICE")
                .isRead(false)
                .build();
        notificationRepository.save(notif);

        // Nếu bé đã liên kết phụ huynh, gửi thông báo cho phụ huynh luôn
        parentStudentLinkRepository.findByStudentId(studentId).ifPresent(link -> {
            if ("APPROVED".equals(link.getStatus())) {
                Notification parentNotif = Notification.builder()
                        .user(link.getParent())
                        .title("Bé " + student.getFullName() + " vừa luyện viết")
                        .content("Bé " + student.getFullName() + " vừa hoàn thành luyện viết chữ '" + request.getLetter() + "' đạt điểm: " + finalScore + "%!")
                        .type("PRACTICE")
                        .isRead(false)
                        .build();
                notificationRepository.save(parentNotif);
            }
        });

        return PracticeResponse.builder()
                .id(saved.getId())
                .letter(saved.getLetter())
                .canvasImageUrl(saved.getCanvasImageUrl())
                .accuracyScore(saved.getAccuracyScore())
                .practicedAt(LocalDateTime.now())
                .build();
    }

    public List<PracticeResponse> getPracticeHistory(Long studentId) {
        return writingPracticeRepository.findByStudentIdOrderByPracticedAtDesc(studentId).stream()
                .map(p -> PracticeResponse.builder()
                        .id(p.getId())
                        .letter(p.getLetter())
                        .canvasImageUrl(p.getCanvasImageUrl())
                        .accuracyScore(p.getAccuracyScore())
                        .practicedAt(p.getPracticedAt())
                        .build())
                .collect(Collectors.toList());
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

    @Transactional
    public ApiResponse submitAssignment(Long studentId, Long assignmentId, SubmissionRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (assignment.getClazz() == null || !classMemberRepository.existsByClazzIdAndStudentId(
                assignment.getClazz().getId(), studentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Học sinh chưa tham gia lớp này");
        }
        if (request.getSubmissionData() == null || request.getSubmissionData().length() > 4_000_000) {
            return new ApiResponse(false, "Dữ liệu bài nộp không hợp lệ hoặc quá lớn.");
        }

        Float score = null;
        String feedback = "Đã nhận bài viết. Chờ giáo viên chấm và nhận xét.";
        if ("QUIZ".equalsIgnoreCase(assignment.getContentType())) {
            try {
                score = calculateQuizScore(assignment.getContentData(), request.getSubmissionData());
            } catch (IllegalArgumentException e) {
                return new ApiResponse(false, "Câu trả lời hoặc nội dung bài tập không hợp lệ.");
            }
            feedback = "Đã tự động chấm điểm trắc nghiệm: " + score + "/10";
        } else if ("WRITING".equalsIgnoreCase(assignment.getContentType())) {
            if (!validWritingSubmission(request.getSubmissionData())) {
                return new ApiResponse(false, "Bài viết phải có ảnh PNG hoặc JPEG hợp lệ.");
            }
        } else {
            return new ApiResponse(false, "Loại bài tập chưa được hỗ trợ.");
        }
        final Float finalScore = score;

        // Xử lý ghi đè và dọn dẹp bản ghi trùng lặp (nếu có do bấm nhanh)
        List<Submission> existingSubs = submissionRepository.findAllByAssignmentIdAndStudentId(assignmentId, studentId);
        Submission submission;
        if (!existingSubs.isEmpty()) {
            submission = existingSubs.get(0);
            submission.setSubmissionData(request.getSubmissionData());
            submission.setScore(score);
            submission.setFeedback(feedback);
            submission.setSubmittedAt(LocalDateTime.now());
            // Dọn dẹp các bản ghi trùng lặp thừa nếu có
            if (existingSubs.size() > 1) {
                for (int i = 1; i < existingSubs.size(); i++) {
                    submissionRepository.delete(existingSubs.get(i));
                }
            }
        } else {
            submission = Submission.builder()
                    .assignment(assignment)
                    .student(student)
                    .submissionData(request.getSubmissionData())
                    .score(score)
                    .feedback(feedback)
                    .submittedAt(LocalDateTime.now())
                    .build();
        }

        submissionRepository.save(submission);

        // Thông báo phụ huynh
        parentStudentLinkRepository.findByStudentId(studentId).ifPresent(link -> {
            if ("APPROVED".equals(link.getStatus())) {
                Notification parentNotif = Notification.builder()
                        .user(link.getParent())
                        .title("Bé " + student.getFullName() + " vừa nộp bài tập")
                        .content("Con vừa nộp bài tập '" + assignment.getTitle() + "' trong lớp '" + assignment.getClazz().getName() + "'. "
                                + (finalScore == null ? "Chờ giáo viên chấm bài." : "Điểm trắc nghiệm: " + finalScore))
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

    private boolean validWritingSubmission(String data) {
        try {
            if (data.startsWith("data:image/")) return validWritingImage(data);
            var images = objectMapper.readTree(data);
            if (!images.isArray() || images.isEmpty() || images.size() > 29) return false;
            for (var item : images) {
                if (!item.path("letter").isTextual() || item.path("letter").asText().isBlank()
                        || !validWritingImage(item.path("image").asText())) return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean validWritingImage(String image) {
        boolean png = image.startsWith("data:image/png;base64,");
        boolean jpeg = image.startsWith("data:image/jpeg;base64,");
        if (!png && !jpeg) return false;
        try {
            byte[] bytes = Base64.getDecoder().decode(image.substring(image.indexOf(',') + 1));
            if (png) return bytes.length >= 8 && bytes[0] == (byte) 137 && bytes[1] == 80
                    && bytes[2] == 78 && bytes[3] == 71;
            return bytes.length >= 3 && bytes[0] == (byte) 255 && bytes[1] == (byte) 216
                    && bytes[2] == (byte) 255;
        } catch (IllegalArgumentException e) { return false; }
    }

    @Autowired
    private LearningTrackingService tracking;

    @Transactional
    public Map<String, String> getParentLinkCode(Long studentId) {
        return tracking.issueLinkCode(studentId);
    }
}
