package com.hocchu.web.repository;

import com.hocchu.web.entity.ParentStudentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, Long> {
    List<ParentStudentLink> findByParentId(Long parentId);
    List<ParentStudentLink> findByParentIdAndStatus(Long parentId, String status);
    Optional<ParentStudentLink> findByStudentId(Long studentId);
    List<ParentStudentLink> findAllByStudentId(Long studentId);
    Optional<ParentStudentLink> findByLinkCode(String linkCode);
    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);
    Optional<ParentStudentLink> findByParentIdAndStudentId(Long parentId, Long studentId);
}
