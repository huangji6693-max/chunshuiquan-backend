package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.DailyCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    Optional<DailyCheckIn> findByUserIdAndCheckDate(UUID userId, LocalDate checkDate);

    /** 获取最近N天签到记录 */
    List<DailyCheckIn> findByUserIdAndCheckDateBetweenOrderByCheckDateDesc(
            UUID userId, LocalDate from, LocalDate to);

    /** 获取最近一次签到 */
    Optional<DailyCheckIn> findFirstByUserIdOrderByCheckDateDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
