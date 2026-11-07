package com.hocchu.web.repository;

import com.hocchu.web.entity.AlphabetLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AlphabetLessonRepository extends JpaRepository<AlphabetLesson, Long> {
    Optional<AlphabetLesson> findByLetter(String letter);
}
