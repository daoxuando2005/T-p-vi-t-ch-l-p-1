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
    private ClassMemberRepository classMemberRepository;

    @org.mockito.Spy private com.hocchu.web.service.HandwritingAssessment handwriting = new com.hocchu.web.service.HandwritingAssessment();
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
        when(assignmentRepository.findForSubmissionById(assignmentId)).thenReturn(Optional.of(assignment));
        when(classMemberRepository.existsByClazzIdAndStudentId(3L, studentId)).thenReturn(true);

        ApiResponse response = studentService.submitAssignment(studentId, assignmentId, request);

        assertTrue(response.getSuccess());
        assertTrue(response.getMessage().contains("Điểm số: 5.0"));
        verify(submissionRepository, times(1)).save(any(Submission.class));
    }

    @Test
    void studentCannotSubmitToAnotherClass() {
        Assignment assignment = assignment("WRITING", "A");
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(assignmentRepository.findForSubmissionById(2L)).thenReturn(Optional.of(assignment));
        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionData("anything");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> studentService.submitAssignment(1L, 2L, request));
        verify(submissionRepository,never()).save(any());
    }

    @Test
    void writingUsesActualGeometryAndRetainsEvidence() {
        prepare(assignment("WRITING", "A"));
        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionData("[{\"letter\":\"A\",\"image\":\""+png()+"\"}]");
        ApiResponse response = studentService.submitAssignment(1L, 2L, request);
        assertTrue(response.getSuccess());
        var saved = org.mockito.ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(saved.capture());
        assertNotNull(saved.getValue().getScore());
        assertEquals("AUTO",saved.getValue().getGradeSource());
        assertTrue(saved.getValue().getScore()<3.5f);
        assertEquals(request.getSubmissionData(), saved.getValue().getSubmissionData());
        assertTrue(saved.getValue().getFeedback().contains("bám mẫu"));
    }

    @Test
    void incompleteQuizIsRejectedWithoutCreatingSubmission() {
        prepare(assignment("QUIZ", "[{\"options\":[\"A\",\"B\"],\"answer\":\"A\"}]"));
        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionData("{\"answers\":[]}");
        assertFalse(studentService.submitAssignment(1L, 2L, request).getSuccess());
        verify(submissionRepository,never()).save(any());
    }

    @Test
    void invalidWritingImageIsRejectedInsteadOfReceivingRandomGrade() {
        prepare(assignment("WRITING", "A"));
        SubmissionRequest request = new SubmissionRequest();
        request.setSubmissionData("[{\"letter\":\"A\",\"image\":\"not-an-image\"}]");
        assertFalse(studentService.submitAssignment(1L, 2L, request).getSuccess());
        verify(submissionRepository,never()).save(any());
    }

    private Assignment assignment(String type, String data) {
        return Assignment.builder().id(2L).title("Bài kiểm thử").contentType(type).contentData(data)
                .clazz(Clazz.builder().id(3L).name("Lớp thử").teacher(User.builder().id(4L).build()).build()).build();
    }
    private String png() {
        try{var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(32,32,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);
            return "data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(output.toByteArray());
        }catch(Exception e){throw new AssertionError(e);}
    }
    @Test void submittedWritingMustCoverExactlyTheAssignedLetters() {
        prepare(assignment("WRITING","{\"letters\":[\"A\",\"Ă\"]}"));var request=new SubmissionRequest();
        request.setSubmissionData("[{\"letter\":\"A\",\"image\":\""+png()+"\"}]");
        assertFalse(studentService.submitAssignment(1L,2L,request).getSuccess());verify(submissionRepository,never()).save(any());
    }
    @Test void retryDoesNotOverwriteTeachersGradeOrDeleteHistory() {
        prepare(assignment("WRITING","A"));var request=new SubmissionRequest();request.setSubmissionData("[{\"letter\":\"A\",\"image\":\""+png()+"\"}]");
        var graded=Submission.builder().submissionData(request.getSubmissionData()).score(8.5f).feedback("Cô đã chấm").build();
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(2L,1L)).thenReturn(Optional.of(graded));
        assertTrue(studentService.submitAssignment(1L,2L,request).getSuccess());assertEquals(8.5f,graded.getScore());
        verify(submissionRepository,never()).save(any());verify(submissionRepository,never()).delete(any());
    }

    @Test
    void assignmentListIncludesTeacherFeedbackAndCurrentGrade() {
        Assignment assignment = assignment("WRITING", "A");
        when(classMemberRepository.findByStudentId(1L)).thenReturn(java.util.List.of(
                ClassMember.builder().clazz(assignment.getClazz()).build()));
        when(assignmentRepository.findByClazzId(3L)).thenReturn(java.util.List.of(assignment));
        when(submissionRepository.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(2L, 1L))
                .thenReturn(Optional.of(Submission.builder().score(8.5f).feedback("Cô đã xem bài").build()));
        AssignmentResponse response = studentService.getAssignments(1L).get(0);
        assertEquals(8.5f, response.getScore());
        assertEquals("Cô đã xem bài", response.getFeedback());
        assertTrue(response.getIsSubmitted());
    }
    private void prepare(Assignment assignment) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).fullName("Học sinh thử").build()));
        when(assignmentRepository.findForSubmissionById(2L)).thenReturn(Optional.of(assignment));
        when(classMemberRepository.existsByClazzIdAndStudentId(3L, 1L)).thenReturn(true);
    }
}
