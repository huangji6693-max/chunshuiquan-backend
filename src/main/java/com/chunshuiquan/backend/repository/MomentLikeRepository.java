package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.MomentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {

    Optional<MomentLike> findByMomentIdAndUserId(UUID momentId, UUID userId);

    boolean existsByMomentIdAndUserId(UUID momentId, UUID userId);

    /** 批量查询：当前用户在哪些动态中点过赞（返回已点赞的 momentId 集合） */
    @Query("SELECT ml.momentId FROM MomentLike ml WHERE ml.userId = :userId AND ml.momentId IN :momentIds")
    List<UUID> findLikedMomentIds(@Param("userId") UUID userId, @Param("momentIds") List<UUID> momentIds);

    void deleteByMomentId(UUID momentId);
}
