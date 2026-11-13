package com.hocchu.web.service;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherService {

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

    @Transactional
    public ClassResponse createClass(Long teacherId, String className, String description) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản giáo viên"));

        // Sinh mã mời ngẫu nhiên 6 ký tự viết hoa duy nhất
        String inviteCode;
        do {
            inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (clazzRepository.findByInviteCode(inviteCode).isPresent());

        Clazz clazz = Clazz.builder()
                .name(className)
                .description(description)
                .inviteCode(inviteCode)
                .teacher(teacher)
                .build();

        Clazz saved = clazzRepository.save(clazz);

        return ClassResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .inviteCode(saved.getInviteCode())
                .teacherId(teacher.getId())
                .teacherName(teacher.getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getMyClasses(Long teacherId) {
        return clazzRepository.findByTeacherId(teacherId).stream()
                .map(c -> ClassResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .inviteCode(c.getInviteCode())
                        .teacherId(teacherId)
                        .teacherName(c.getTeacher().getFullName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getClassStudents(Long teacherId, Long classId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        return classMemberRepository.findByClazzId(classId).stream()
                .map(cm -> {
                    User s = cm.getStudent();
                    List<String> roles = s.getRoles().stream().map(Role::getName).collect(Collectors.toList());
                    return new UserProfileResponse(s.getId(), s.getUsername(), s.getEmail(), s.getFullName(), s.getPhoneNumber(), roles);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse addStudentToClass(Long teacherId, Long classId, String studentUsername) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh với username: " + studentUsername));

        // Kiểm tra xem có đúng vai trò học sinh không
        boolean isStudent = student.getRoles().stream().anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));
        if (!isStudent) {
            return new ApiResponse(false, "Người dùng này không phải là học sinh.");
        }

        if (classMemberRepository.existsByClazzIdAndStudentId(classId, student.getId())) {
            return new ApiResponse(false, "Học sinh đã có mặt trong lớp.");
        }

        ClassMember member = ClassMember.builder()
                .clazz(clazz)
                .student(student)
                .build();
        classMemberRepository.save(member);

        return new ApiResponse(true, "Thêm học sinh thành công!");
    }

    @Transactional
    public ApiResponse removeStudentFromClass(Long teacherId, Long classId, Long studentId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        ClassMember member = classMemberRepository.findByClazzIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Học sinh không ở trong lớp học này"));

        classMemberRepository.delete(member);
        return new ApiResponse(true, "Đã xoá học sinh khỏi lớp học thành công");
    }

    @Transactional
    public AssignmentResponse createAssignment(Long teacherId, AssignmentRequest request) {
        if(request.getClassId()==null)throw new IllegalArgumentException("Chọn lớp nhận bài.");
        request.setContentData(AssignmentRules.validate(request.getContentType(),request.getContentData()));
        request.setContentType(request.getContentType().toUpperCase(java.util.Locale.ROOT));
        Clazz clazz = clazzRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        Assignment asm = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .contentType(request.getContentType())
                .contentData(request.getContentData())
                .dueDate(request.getDueDate())
                .clazz(clazz)
                .build();

        Assignment saved = assignmentRepository.save(asm);

        return AssignmentResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .contentType(saved.getContentType())
                .contentData(saved.getContentData())
                .dueDate(saved.getDueDate())
                .classId(clazz.getId())
                .className(clazz.getName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getClassAssignments(Long teacherId, Long classId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        return assignmentRepository.findByClazzId(classId).stream()
                .map(asm -> AssignmentResponse.builder()
                        .id(asm.getId())
                        .title(asm.getTitle())
                        .description(asm.getDescription())
                        .contentType(asm.getContentType())
                        .contentData(asm.getContentData())
                        .dueDate(asm.getDueDate())
                        .classId(classId)
                        .className(clazz.getName())
                        .createdAt(asm.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAssignmentSubmissions(Long teacherId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!assignment.getClazz().getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        List<Submission> allSubs = submissionRepository.findByAssignmentId(assignmentId);
        
        // Nhóm theo từng học sinh để chỉ lấy bản ghi bài nộp mới nhất của mỗi học sinh
        Map<Long, Submission> latestSubsByStudent = new LinkedHashMap<>();
        for (Submission sub : allSubs) {
            if (sub.getStudent() == null) continue;
            Long studentId = sub.getStudent().getId();
            if (!latestSubsByStudent.containsKey(studentId)) {
                latestSubsByStudent.put(studentId, sub);
            } else {
                Submission prev = latestSubsByStudent.get(studentId);
                // Giữ bản ghi có ID lớn hơn hoặc thời gian nộp mới hơn
                if (sub.getId() != null && prev.getId() != null && sub.getId() > prev.getId()) {
                    latestSubsByStudent.put(studentId, sub);
                }
            }
        }

        return latestSubsByStudent.values().stream()
                .map(sub -> SubmissionResponse.builder()
                        .id(sub.getId())
                        .assignmentId(assignmentId)
                        .assignmentTitle(assignment.getTitle())
                        .studentId(sub.getStudent().getId())
                        .studentName(sub.getStudent().getFullName())
                        .submissionData(sub.getSubmissionData())
                        .score(sub.getScore())
                        .autoScore(sub.getAutoScore()).gradeSource(sub.getGradeSource()).assessmentData(sub.getAssessmentData())
                        .feedback(sub.getFeedback())
                        .submittedAt(sub.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse gradeSubmission(Long teacherId, Long submissionId, GradeRequest request) {
        if(request.getScore()==null||!Float.isFinite(request.getScore())||request.getScore()<0||request.getScore()>10)
            throw new IllegalArgumentException("Điểm nằm trong khoảng 0–10.");
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        Clazz clazz = submission.getAssignment().getClazz();
        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chấm điểm bài tập của lớp này");
        }

        submission.setScore(request.getScore());
        submission.setGradeSource("TEACHER");
        submission.setFeedback(request.getFeedback());
        submissionRepository.save(submission);

        return new ApiResponse(true, "Chấm điểm thành công!");
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AssignmentResponse updateAssignment(Long teacherId, Long assignmentId, AssignmentRequest request) {
        // Serialize changes with submission receipt so the stored answer key stays stable.
        Assignment asm = assignmentRepository.findForSubmissionById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!asm.getClazz().getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không phải giáo viên phụ trách lớp học này");
        }

        if(request.getContentData()!=null||request.getContentType()!=null) {
            String type=request.getContentType()==null?asm.getContentType():request.getContentType().toUpperCase(java.util.Locale.ROOT);
            String content=AssignmentRules.validate(type,request.getContentData()==null?asm.getContentData():request.getContentData());
            if(!java.util.Objects.equals(type,asm.getContentType())||!AssignmentRules.sameContent(content,asm.getContentData())) {
                if(!submissionRepository.findByAssignmentId(assignmentId).isEmpty())
                    throw new IllegalArgumentException("Bài đã có học sinh nộp. Hãy tạo bài mới để giữ nguyên đề và kết quả cũ.");
                asm.setContentType(type);asm.setContentData(content);
            }
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            asm.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            asm.setDescription(request.getDescription());
        }
        asm.setDueDate(request.getDueDate());

        Assignment saved = assignmentRepository.save(asm);

        return AssignmentResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .contentType(saved.getContentType())
                .contentData(saved.getContentData())
                .dueDate(saved.getDueDate())
                .classId(saved.getClazz().getId())
                .className(saved.getClazz().getName())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public ApiResponse deleteAssignment(Long teacherId, Long assignmentId) {
        Assignment asm = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!asm.getClazz().getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa bài tập của lớp này");
        }

        // Xóa tất cả bài nộp của bài tập này
        submissionRepository.deleteByAssignmentId(assignmentId);
        
        // Xóa bài tập
        assignmentRepository.delete(asm);

        return new ApiResponse(true, "Xóa bài tập thành công!");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherStats(Long teacherId) {
        List<Clazz> classes = clazzRepository.findByTeacherId(teacherId);
        int totalClasses = classes.size();

        Set<Long> studentIds = new HashSet<>();
        int totalAssignments = 0;
        int totalSubmissions = 0;
        int gradedSubmissions = 0;
        int ungradedSubmissions = 0;

        for (Clazz c : classes) {
            List<ClassMember> members = classMemberRepository.findByClazzId(c.getId());
            for (ClassMember m : members) {
                if (m.getStudent() != null) {
                    studentIds.add(m.getStudent().getId());
                }
            }

            List<Assignment> assignments = assignmentRepository.findByClazzId(c.getId());
            totalAssignments += assignments.size();

            for (Assignment a : assignments) {
                List<Submission> submissions = submissionRepository.findByAssignmentId(a.getId());
                totalSubmissions += submissions.size();
                for (Submission s : submissions) {
                    if (s.getScore() != null) {
                        gradedSubmissions++;
                    } else {
                        ungradedSubmissions++;
                    }
                }
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClasses", totalClasses);
        stats.put("totalStudents", studentIds.size());
        stats.put("totalAssignments", totalAssignments);
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("gradedSubmissions", gradedSubmissions);
        stats.put("ungradedSubmissions", ungradedSubmissions);

        return stats;
    }

    @Transactional
    public ClassResponse updateClass(Long teacherId, Long classId, String className, String description) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền chỉnh sửa lớp học này");
        }

        clazz.setName(className);
        clazz.setDescription(description);
        Clazz saved = clazzRepository.save(clazz);

        return ClassResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .inviteCode(saved.getInviteCode())
                .teacherId(teacherId)
                .teacherName(clazz.getTeacher().getFullName())
                .build();
    }

    @Transactional
    public ApiResponse deleteClass(Long teacherId, Long classId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xóa lớp học này");
        }

        // Tận dụng ON DELETE CASCADE của cơ sở dữ liệu MySQL để tự động xóa sạch các bản ghi liên quan ở các bảng:
        // class_members, assignments, submissions.
        clazzRepository.delete(clazz);

        return new ApiResponse(true, "Xóa lớp học thành công!");
    }
}

