package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.MomentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {

    Optional<MomentLike> findByMomentIdAndUserId(UUID momentId, UUID userId);

    boolean existsByMomentIdAndUserId(UUID momentId, UUID userId);

    void deleteByMomentId(UUID momentId);
}
