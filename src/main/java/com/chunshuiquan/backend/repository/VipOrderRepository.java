package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.VipOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VipOrderRepository extends JpaRepository<VipOrder, UUID> {

    List<VipOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
