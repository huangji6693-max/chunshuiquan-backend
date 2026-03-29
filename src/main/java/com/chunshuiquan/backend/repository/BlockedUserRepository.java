package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdOrBlockedId(UUID blockerId, UUID blockedId);
}
