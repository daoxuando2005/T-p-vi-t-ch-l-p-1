package com.hocchu.web.repository;

import com.hocchu.web.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignmentId(Long assignmentId);
    List<Submission> findByStudentId(Long studentId);
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    Optional<Submission> findFirstByAssignmentIdAndStudentIdOrderByIdDesc(Long assignmentId, Long studentId);
    List<Submission> findAllByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<Submission> findAllByAssignmentIdAndStudentIdOrderByIdDesc(Long assignmentId, Long studentId);
    void deleteByAssignmentId(Long assignmentId);
}
