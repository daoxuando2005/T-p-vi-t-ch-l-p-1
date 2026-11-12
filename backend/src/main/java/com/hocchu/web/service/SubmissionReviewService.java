package com.hocchu.web.service;

import com.hocchu.web.repository.SubmissionRepository;
import com.hocchu.web.entity.Submission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class SubmissionReviewService {
    @Autowired private SubmissionRepository submissions;
    @Autowired private LearningTrackingService tracking;
    public record Detail(Long id,Long studentId,String title,String type,String instructions,String assignmentContent,
        String submissionData,Float score,String feedback,LocalDateTime submittedAt,Float autoScore,String gradeSource,String assessmentData) {}
    @Transactional(readOnly=true)
    public Detail student(Long studentId,Long assignmentId) {
        return detail(submissions.findFirstByAssignmentIdAndStudentIdOrderByIdDesc(assignmentId,studentId)
            .orElseThrow(()->new AccessDeniedException("Không có bài nộp của tài khoản này.")));
    }
    @Transactional(readOnly=true)
    public Detail parent(Long parentId,Long studentId,Long submissionId) {
        tracking.requireLinked(parentId,studentId);
        var submission=submissions.findById(submissionId).filter(s->s.getStudent().getId().equals(studentId))
            .orElseThrow(()->new AccessDeniedException("Không có quyền xem bài nộp này."));
        return detail(submission);
    }
    private static Detail detail(Submission s) {
        var a=s.getAssignment();
        return new Detail(s.getId(),s.getStudent().getId(),a.getTitle(),a.getContentType(),a.getDescription(),a.getContentData(),
            s.getSubmissionData(),s.getScore(),s.getFeedback(),s.getSubmittedAt(),s.getAutoScore(),s.getGradeSource(),s.getAssessmentData());
    }
}
