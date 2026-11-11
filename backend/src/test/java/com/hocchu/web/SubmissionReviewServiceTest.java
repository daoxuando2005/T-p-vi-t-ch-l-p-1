package com.hocchu.web;

import com.hocchu.web.entity.*;
import com.hocchu.web.repository.SubmissionRepository;
import com.hocchu.web.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionReviewServiceTest {
    @Mock SubmissionRepository submissions;
    @Mock LearningTrackingService tracking;
    @InjectMocks SubmissionReviewService service;
    @Test void unlinkedParentCannotEvenQuerySubmission() {
        doThrow(new AccessDeniedException("denied")).when(tracking).requireLinked(10L,2L);
        assertThrows(AccessDeniedException.class,()->service.parent(10L,2L,3L));verifyNoInteractions(submissions);
    }
    @Test void linkedParentCannotReadAnotherChildsSubmission() {
        when(submissions.findById(3L)).thenReturn(Optional.of(Submission.builder().id(3L).student(User.builder().id(99L).build()).build()));
        assertThrows(AccessDeniedException.class,()->service.parent(10L,2L,3L));
    }
    @Test void pendingWritingReviewRetainsActualImageAndNullScore() {
        var assignment=Assignment.builder().title("Viết A").contentType("WRITING").contentData("A").build();
        var submission=Submission.builder().id(3L).student(User.builder().id(2L).build()).assignment(assignment)
            .submissionData("actual-student-image").feedback("Chờ cô chấm").build();
        when(submissions.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(8L,2L)).thenReturn(Optional.of(submission));
        var detail=service.student(2L,8L);assertNull(detail.score());assertEquals("actual-student-image",detail.submissionData());
    }
}
