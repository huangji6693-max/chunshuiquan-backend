package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    /** 查询用户金币流水，按时间降序 */
    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** 注销时删除 */
    void deleteByUserId(UUID userId);
}
