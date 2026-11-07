package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
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
public class ParentService {

    @Autowired
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WritingPracticeRepository writingPracticeRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private ClassMemberRepository classMemberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Transactional
    public ApiResponse linkStudent(Long parentId, String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ApiResponse(false, "Vui lÄ‚Â²ng nhĂ¡ÂºÂ­p tÄ‚Âªn Ă„â€˜Ă„Æ’ng nhĂ¡ÂºÂ­p cĂ¡Â»Â§a bÄ‚Â© (vd: student1) hoĂ¡ÂºÂ·c mÄ‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t!");
        }

        String searchKey = input.trim();
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("KhÄ‚Â´ng tÄ‚Â¬m thĂ¡ÂºÂ¥y tÄ‚Â i khoĂ¡ÂºÂ£n phĂ¡Â»Â¥ huynh"));

        // 1. ThĂ¡Â»Â­ tÄ‚Â¬m hĂ¡Â»Âc sinh theo tÄ‚Âªn Ă„â€˜Ă„Æ’ng nhĂ¡ÂºÂ­p (Username)
        Optional<User> studentOpt = userRepository.findByUsername(searchKey);
        if (studentOpt.isPresent()) {
            User student = studentOpt.get();
            boolean isStudentRole = student.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));
            if (!isStudentRole) {
                return new ApiResponse(false, "TÄ‚Âªn Ă„â€˜Ă„Æ’ng nhĂ¡ÂºÂ­p '" + searchKey + "' khÄ‚Â´ng phĂ¡ÂºÂ£i lÄ‚Â  tÄ‚Â i khoĂ¡ÂºÂ£n hĂ¡Â»Âc sinh.");
            }

            Optional<ParentStudentLink> existingLinkOpt = parentStudentLinkRepository.findByStudentId(student.getId());
            if (existingLinkOpt.isPresent()) {
                ParentStudentLink link = existingLinkOpt.get();
                if ("APPROVED".equals(link.getStatus()) && link.getParent() != null && parent.getId().equals(link.getParent().getId())) {
                    return new ApiResponse(false, "BĂ¡ÂºÂ¡n Ă„â€˜Ä‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t thÄ‚Â nh cÄ‚Â´ng vĂ¡Â»â€ºi tÄ‚Â i khoĂ¡ÂºÂ£n bÄ‚Â© '" + (student.getFullName() != null ? student.getFullName() : student.getUsername()) + "' tĂ¡Â»Â« trĂ†Â°Ă¡Â»â€ºc rĂ¡Â»â€œi!");
                }
                link.setParent(parent);
                link.setStatus("APPROVED");
                link.setLinkedAt(LocalDateTime.now());
                parentStudentLinkRepository.save(link);
            } else {
                ParentStudentLink newLink = ParentStudentLink.builder()
                        .parent(parent)
                        .student(student)
                        .linkCode("LINK-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                        .status("APPROVED")
                        .linkedAt(LocalDateTime.now())
                        .build();
                parentStudentLinkRepository.save(newLink);
            }

            createLinkNotifications(parent, student);
            return new ApiResponse(true, "LiÄ‚Âªn kĂ¡ÂºÂ¿t vĂ¡Â»â€ºi tÄ‚Â i khoĂ¡ÂºÂ£n bÄ‚Â© '" + (student.getFullName() != null ? student.getFullName() : student.getUsername()) + "' thÄ‚Â nh cÄ‚Â´ng!");
        }

        // 2. ThĂ¡Â»Â­ tÄ‚Â¬m theo MÄ‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t (Link code)
        Optional<ParentStudentLink> linkOpt = parentStudentLinkRepository.findByLinkCode(searchKey);
        if (linkOpt.isPresent()) {
            ParentStudentLink link = linkOpt.get();
            if ("APPROVED".equals(link.getStatus()) && link.getParent() != null && parent.getId().equals(link.getParent().getId())) {
                return new ApiResponse(false, "MÄ‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t nÄ‚Â y Ă„â€˜Ä‚Â£ Ă„â€˜Ă†Â°Ă¡Â»Â£c bĂ¡ÂºÂ¡n sĂ¡Â»Â­ dĂ¡Â»Â¥ng trĂ†Â°Ă¡Â»â€ºc Ă„â€˜Ä‚Â³.");
            }

            link.setParent(parent);
            link.setStatus("APPROVED");
            link.setLinkedAt(LocalDateTime.now());
            parentStudentLinkRepository.save(link);

            createLinkNotifications(parent, link.getStudent());
            return new ApiResponse(true, "LiÄ‚Âªn kĂ¡ÂºÂ¿t vĂ¡Â»â€ºi bÄ‚Â© '" + (link.getStudent().getFullName() != null ? link.getStudent().getFullName() : link.getStudent().getUsername()) + "' thÄ‚Â nh cÄ‚Â´ng!");
        }

        return new ApiResponse(false, "KhÄ‚Â´ng tÄ‚Â¬m thĂ¡ÂºÂ¥y hĂ¡Â»Âc sinh vĂ¡Â»â€ºi tÄ‚Âªn Ă„â€˜Ă„Æ’ng nhĂ¡ÂºÂ­p hoĂ¡ÂºÂ·c mÄ‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t: " + searchKey);
    }

    private void createLinkNotifications(User parent, User student) {
        String studentDisplayName = student.getFullName() != null ? student.getFullName() : student.getUsername();
        String parentDisplayName = parent.getFullName() != null ? parent.getFullName() : parent.getUsername();

        Notification parentNotif = Notification.builder()
                .user(parent)
                .title("LiÄ‚Âªn kĂ¡ÂºÂ¿t tÄ‚Â i khoĂ¡ÂºÂ£n con thÄ‚Â nh cÄ‚Â´ng")
                .content("BĂ¡ÂºÂ¡n Ă„â€˜Ä‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t thÄ‚Â nh cÄ‚Â´ng vĂ¡Â»â€ºi tÄ‚Â i khoĂ¡ÂºÂ£n bÄ‚Â© '" + studentDisplayName + "'!")
                .type("SYSTEM")
                .isRead(false)
                .build();
        notificationRepository.save(parentNotif);

        Notification studentNotif = Notification.builder()
                .user(student)
                .title("TÄ‚Â i khoĂ¡ÂºÂ£n Ă„â€˜Ä‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t vĂ¡Â»â€ºi phĂ¡Â»Â¥ huynh")
                .content("TÄ‚Â i khoĂ¡ÂºÂ£n cĂ¡Â»Â§a con Ă„â€˜Ä‚Â£ liÄ‚Âªn kĂ¡ÂºÂ¿t thÄ‚Â nh cÄ‚Â´ng vĂ¡Â»â€ºi phĂ¡Â»Â¥ huynh '" + parentDisplayName + "'!")
                .type("SYSTEM")
                .isRead(false)
                .build();
        notificationRepository.save(studentNotif);
    }

    @Transactional(readOnly = true)
    public List<ParentLinkResponse> getMyStudents(Long parentId) {
        return parentStudentLinkRepository.findByParentIdAndStatus(parentId, "APPROVED").stream()
                .map(link -> ParentLinkResponse.builder()
                        .id(link.getId())
                        .parentId(parentId)
                        .parentName(link.getParent().getFullName() != null ? link.getParent().getFullName() : link.getParent().getUsername())
                        .studentId(link.getStudent().getId())
                        .studentName(link.getStudent().getFullName() != null ? link.getStudent().getFullName() : link.getStudent().getUsername())
                        .studentUsername(link.getStudent().getUsername())
                        .linkCode(link.getLinkCode())
                        .status(link.getStatus())
                        .linkedAt(link.getLinkedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgress(Long parentId, Long studentId) {
        boolean isLinked = parentStudentLinkRepository.existsByParentIdAndStudentId(parentId, studentId);
        if (!isLinked) {
            throw new RuntimeException("BĂ¡ÂºÂ¡n khÄ‚Â´ng cÄ‚Â³ quyĂ¡Â»Ân xem thÄ‚Â´ng tin cĂ¡Â»Â§a hĂ¡Â»Âc sinh nÄ‚Â y");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("KhÄ‚Â´ng tÄ‚Â¬m thĂ¡ÂºÂ¥y hĂ¡Â»Âc sinh"));

        List<WritingPractice> practices = writingPracticeRepository.findByStudentIdOrderByPracticedAtDesc(studentId);
        int totalPractices = practices.size();
        float avgPracticeScore = 0.0f;
        if (totalPractices > 0) {
            double sum = practices.stream().mapToDouble(WritingPractice::getAccuracyScore).sum();
            avgPracticeScore = (float) (sum / totalPractices);
            avgPracticeScore = Math.round(avgPracticeScore * 10.0f) / 10.0f;
        }

        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        int totalSubmissions = submissions.size();
        float avgSubmissionScore = 0.0f;
        if (totalSubmissions > 0) {
            double sum = submissions.stream().mapToDouble(Submission::getScore).sum();
            avgSubmissionScore = (float) (sum / totalSubmissions);
            avgSubmissionScore = Math.round(avgSubmissionScore * 10.0f) / 10.0f;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        List<StudentProgressResponse.PracticePoint> practiceHistory = practices.stream()
                .limit(10)
                .map(p -> new StudentProgressResponse.PracticePoint(
                        p.getLetter(),
                        p.getAccuracyScore(),
                        p.getPracticedAt() != null ? p.getPracticedAt().format(formatter) : LocalDateTime.now().format(formatter)
                ))
                .collect(Collectors.toList());
        Collections.reverse(practiceHistory);

        List<StudentProgressResponse.SubmissionPoint> submissionHistory = submissions.stream()
                .limit(10)
                .map(s -> new StudentProgressResponse.SubmissionPoint(
                        s.getAssignment().getTitle(),
                        s.getScore(),
                        s.getSubmittedAt() != null ? s.getSubmittedAt().format(formatter) : LocalDateTime.now().format(formatter)
                ))
                .collect(Collectors.toList());
        Collections.reverse(submissionHistory);

        return StudentProgressResponse.builder()
                .studentId(studentId)
                .studentName(student.getFullName() != null ? student.getFullName() : student.getUsername())
                .totalPractices(totalPractices)
                .averagePracticeScore(avgPracticeScore)
                .totalSubmissions(totalSubmissions)
                .averageSubmissionScore(avgSubmissionScore)
                .practiceHistory(practiceHistory)
                .submissionHistory(submissionHistory)
                .build();
    }

    @Transactional(readOnly = true)
    public List<com.hocchu.web.dto.ClassmateScoreResponse> getClassmatesScores(Long parentId, Long studentId) {
        boolean isLinked = parentStudentLinkRepository.existsByParentIdAndStudentId(parentId, studentId);
        if (!isLinked) {
            throw new RuntimeException("Bạn không có quyền xem thông tin của học sinh này");
        }
        
        List<ClassMember> memberships = classMemberRepository.findByStudentId(studentId);
        if (memberships.isEmpty()) return new ArrayList<>();
        
        // Lấy lớp đầu tiên
        Clazz clazz = memberships.get(0).getClazz();
        List<ClassMember> allMembers = classMemberRepository.findByClazzId(clazz.getId());
        
        List<com.hocchu.web.dto.ClassmateScoreResponse> result = new ArrayList<>();
        for (ClassMember member : allMembers) {
            User s = member.getStudent();
            if (s == null) continue;
            
            List<Submission> submissions = submissionRepository.findByStudentId(s.getId());
            float totalScore = 0f;
            int completedCount = 0;
            
            for (Submission sub : submissions) {
                if (sub.getScore() != null) {
                    totalScore += sub.getScore();
                    completedCount++;
                }
            }
            
            result.add(com.hocchu.web.dto.ClassmateScoreResponse.builder()
                .studentId(s.getId())
                .fullName(s.getFullName())
                .username(s.getUsername())
                .totalScore(totalScore)
                .totalAssignmentsCompleted(completedCount)
                .build());
        }
        
        result.sort((a, b) -> Float.compare(b.getTotalScore(), a.getTotalScore())); // Sort descending
        return result;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getStudentAssignments(Long parentId, Long studentId) {
        boolean isLinked = parentStudentLinkRepository.existsByParentIdAndStudentId(parentId, studentId);
        if (!isLinked) {
            throw new RuntimeException("Bạn không có quyền xem bài tập của học sinh này");
        }

        List<ClassMember> memberships = classMemberRepository.findByStudentId(studentId);
        List<AssignmentResponse> result = new ArrayList<>();

        for (ClassMember cm : memberships) {
            Clazz clazz = cm.getClazz();
            if (clazz == null) continue;
            List<Assignment> assignments = assignmentRepository.findByClazzId(clazz.getId());
            for (Assignment asm : assignments) {
                Optional<Submission> subOpt = submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(asm.getId(), studentId);
                result.add(AssignmentResponse.builder()
                        .id(asm.getId())
                        .title(asm.getTitle())
                        .description(asm.getDescription())
                        .contentType(asm.getContentType())
                        .contentData(asm.getContentData())
                        .dueDate(asm.getDueDate())
                        .classId(clazz.getId())
                        .className(clazz.getName())
                        .createdAt(asm.getCreatedAt())
                        .isSubmitted(subOpt.isPresent())
                        .score(subOpt.map(Submission::getScore).orElse(null))
                        .feedback(subOpt.map(Submission::getFeedback).orElse(null))
                        .submissionData(subOpt.map(Submission::getSubmissionData).orElse(null))
                        .assignedBy(asm.getAssignedBy() != null ? asm.getAssignedBy() : "TEACHER")
                        .build());
            }
        }
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return result;
    }

    @Transactional
    public ApiResponse remindStudentAssignment(Long parentId, Long studentId, Long assignmentId) {
        boolean isLinked = parentStudentLinkRepository.existsByParentIdAndStudentId(parentId, studentId);
        if (!isLinked) {
            throw new RuntimeException("Bạn không có quyền nhắc nhở học sinh này");
        }

        User parent = userRepository.findById(parentId).orElseThrow(() -> new RuntimeException("Không tìm thấy phụ huynh"));
        User student = userRepository.findById(studentId).orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));
        Assignment asm = assignmentRepository.findById(assignmentId).orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        String parentName = parent.getFullName() != null ? parent.getFullName() : parent.getUsername();
        String className = asm.getClazz() != null ? asm.getClazz().getName() : "Lớp học";

        Notification studentNotif = Notification.builder()
                .user(student)
                .title("Bố/Mẹ nhắc nhở con làm bài tập ⏰")
                .content(parentName + " nhắc con làm bài tập '" + asm.getTitle() + "' (Lớp " + className + ") ngay nhé! ❤️")
                .type("ASSIGNMENT")
                .isRead(false)
                .build();
        notificationRepository.save(studentNotif);

        return new ApiResponse(true, "Đã gửi thông báo nhắc nhở làm bài tập tới tài khoản của bé!");
    }

    @Transactional
    public ApiResponse assignHomeworkToChild(Long parentId, Long studentId, String title, String description, String dueDateStr, String letters) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản phụ huynh"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh"));

        // Kiểm tra liên kết Phụ huynh - Học sinh
        Optional<ParentStudentLink> linkOpt = parentStudentLinkRepository.findByParentIdAndStudentId(parentId, studentId);
        if (linkOpt.isEmpty() || !"APPROVED".equals(linkOpt.get().getStatus())) {
            return new ApiResponse(false, "Bạn chưa liên kết với tài khoản học sinh này!");
        }

        // Tìm lớp học của bé để liên kết bài tập (nếu có)
        List<ClassMember> members = classMemberRepository.findByStudentId(studentId);
        Clazz clazz = members.isEmpty() ? null : members.get(0).getClazz();

        LocalDateTime dueDate = LocalDateTime.now().plusDays(3);
        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            try {
                dueDate = LocalDateTime.parse(dueDateStr + "T23:59:59");
            } catch (Exception e) {
            }
        }

        String contentDataJson = "{\"letters\": [\"A\"], \"targetScore\": 50}";
        if (letters != null && !letters.trim().isEmpty()) {
            String[] arr = letters.split(",");
            StringBuilder sb = new StringBuilder("{\"letters\": [");
            for (int i=0; i<arr.length; i++) {
                sb.append("\"").append(arr[i].trim()).append("\"");
                if (i < arr.length - 1) sb.append(",");
            }
            sb.append("], \"targetScore\": 50}");
            contentDataJson = sb.toString();
        }

        Assignment asm = Assignment.builder()
                .title(title)
                .description(description != null && !description.trim().isEmpty() ? description : "Bài tập do Phụ huynh " + parent.getFullName() + " giao.")
                .contentType("WRITING")
                .contentData(contentDataJson)
                .dueDate(dueDate)
                .clazz(clazz)
                .assignedBy("PARENT")
                .build();

        Assignment saved = assignmentRepository.save(asm);

        // Gửi thông báo đến Quả chuông của bé
        Notification notif = Notification.builder()
                .user(student)
                .title("🔔 Phụ huynh vừa giao bài tập mới cho bé!")
                .content(parent.getFullName() + " vừa giao cho bé bài tập tập viết '" + saved.getTitle() + "'!")
                .type("ASSIGNMENT")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notif);

        return new ApiResponse(true, "Giao bài tập thành công!");
    }

    @Transactional
    public ApiResponse updateAssignment(Long parentId, Long assignmentId, String title, String dueDateStr, String letters) {
        Assignment asm = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!"PARENT".equals(asm.getAssignedBy())) {
            throw new RuntimeException("Bạn không có quyền sửa bài tập này");
        }

        if (title != null && !title.trim().isEmpty()) {
            asm.setTitle(title);
        }

        if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
            try {
                asm.setDueDate(LocalDateTime.parse(dueDateStr + "T23:59:59"));
            } catch (Exception e) {}
        } else if (dueDateStr != null) {
            asm.setDueDate(null);
        }

        if (letters != null && !letters.trim().isEmpty()) {
            String[] arr = letters.split(",");
            StringBuilder sb = new StringBuilder("{\"letters\": [");
            for (int i=0; i<arr.length; i++) {
                sb.append("\"").append(arr[i].trim()).append("\"");
                if (i < arr.length - 1) sb.append(",");
            }
            sb.append("], \"targetScore\": 50}");
            asm.setContentData(sb.toString());
        }

        assignmentRepository.save(asm);
        return new ApiResponse(true, "Cập nhật bài tập thành công!");
    }

    @Transactional
    public ApiResponse deleteAssignment(Long parentId, Long assignmentId) {
        Assignment asm = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!"PARENT".equals(asm.getAssignedBy())) {
            throw new RuntimeException("Bạn không có quyền xóa bài tập này");
        }

        submissionRepository.deleteByAssignmentId(assignmentId);
        assignmentRepository.delete(asm);

        return new ApiResponse(true, "Xóa bài tập thành công!");
    }
}
