package com.hocchu.web.repository;

import com.hocchu.web.entity.FeedbackReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {
    List<FeedbackReport> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<FeedbackReport> findAllByOrderByCreatedAtDesc();
}
