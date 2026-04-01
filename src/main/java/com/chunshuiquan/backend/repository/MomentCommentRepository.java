package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.MomentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MomentCommentRepository extends JpaRepository<MomentComment, UUID> {

    List<MomentComment> findByMomentIdOrderByCreatedAtAsc(UUID momentId);

    void deleteByMomentId(UUID momentId);
}
