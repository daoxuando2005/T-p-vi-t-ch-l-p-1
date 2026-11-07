package com.hocchu.web.repository;

import com.hocchu.web.entity.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {
    List<Clazz> findByTeacherId(Long teacherId);
    Optional<Clazz> findByInviteCode(String inviteCode);
}
