package com.hocchu.web.repository;

import com.hocchu.web.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByStudentId(Long studentId);
    List<ClassMember> findByClazzId(Long classId);
    Optional<ClassMember> findByClazzIdAndStudentId(Long classId, Long studentId);
    boolean existsByClazzIdAndStudentId(Long classId, Long studentId);
}
