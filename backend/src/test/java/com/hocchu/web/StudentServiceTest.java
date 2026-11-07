package com.hocchu.web;

import com.hocchu.web.dto.*;
import com.hocchu.web.entity.*;
import com.hocchu.web.repository.*;
import com.hocchu.web.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ParentStudentLinkRepository parentStudentLinkRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    public void testSubmitQuizAssignment_Success() {
        Long studentId = 1L;
        Long assignmentId = 2L;

        User student = User.builder().id(studentId).fullName("Nguyễn Văn An").build();
        Clazz clazz = Clazz.builder().id(3L).name("Lớp 1A").teacher(User.builder().id(4L).build()).build();
        
        // Mock cấu trúc câu hỏi JSON: Có 2 câu hỏi, câu 1 đáp án A, câu 2 đáp án B
        String contentData = "[{\"question\":\"Chữ gì?\",\"options\":[\"A\",\"B\",\"C\"],\"answer\":\"A\"},{\"question\":\"Chữ gì tiếp?\",\"options\":[\"A\",\"B\",\"C\"],\"answer\":\"B\"}]";
        
        Assignment assignment = Assignment.builder()
                .id(assignmentId)
                .title("Trắc nghiệm bảng chữ cái")
                .contentType("QUIZ")
                .contentData(contentData)
                .dueDate(LocalDateTime.now().plusDays(2))
                .clazz(clazz)
                .build();

        // Học sinh trả lời: Câu 1 chọn A (Đúng), Câu 2 chọn A (Sai) -> Điểm = (1/2)*10 = 5.0
        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionData("{\"answers\":[\"A\",\"A\"]}");

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)).thenReturn(Optional.empty());

        ApiResponse response = studentService.submitAssignment(studentId, assignmentId, request);

        assertTrue(response.getSuccess());
        assertTrue(response.getMessage().contains("Điểm số: 5.0"));
        verify(submissionRepository, times(1)).save(any(Submission.class));
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }
}
