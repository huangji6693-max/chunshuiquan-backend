package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationRepository extends JpaRepository<Verification, UUID> {

    Optional<Verification> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Verification> findByStatusOrderByCreatedAtAsc(String status);
}
