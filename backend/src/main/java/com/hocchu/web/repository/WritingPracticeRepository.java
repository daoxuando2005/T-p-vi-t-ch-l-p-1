package com.hocchu.web.repository;

import com.hocchu.web.entity.WritingPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WritingPracticeRepository extends JpaRepository<WritingPractice, Long> {
    List<WritingPractice> findByStudentIdOrderByPracticedAtDesc(Long studentId);
}
