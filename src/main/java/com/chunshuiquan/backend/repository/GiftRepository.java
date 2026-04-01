package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftRepository extends JpaRepository<Gift, Long> {

    /** 获取所有上架中的礼物 */
    List<Gift> findByIsActiveTrueOrderByCoinsAsc();
}
