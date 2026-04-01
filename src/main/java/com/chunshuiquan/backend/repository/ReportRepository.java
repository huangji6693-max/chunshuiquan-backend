package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    void deleteByReporterIdOrReportedId(UUID reporterId, UUID reportedId);

    /** 按状态查询举报，按时间升序（先处理最早的） */
    List<Report> findByStatusOrderByCreatedAtAsc(String status);

    /** 全部举报，按时间降序 */
    List<Report> findAllByOrderByCreatedAtDesc();
}
