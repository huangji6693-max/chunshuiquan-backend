package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MomentRepository extends JpaRepository<Moment, UUID> {

    /** 公开动态时间线 */
    Page<Moment> findByIsDeletedFalseAndVisibilityOrderByCreatedAtDesc(String visibility, Pageable pageable);

    /** 某用户的动态 */
    Page<Moment> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    void deleteByAuthorId(UUID authorId);
}
