package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.GiftRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GiftRecordRepository extends JpaRepository<GiftRecord, UUID> {

    /** 查询我收到的礼物，按时间降序 */
    List<GiftRecord> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    /** 查询我送出的礼物，按时间降序 */
    List<GiftRecord> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    /** 删除与用户相关的礼物记录（注销账号时使用） */
    void deleteBySenderIdOrReceiverId(UUID senderId, UUID receiverId);
}
