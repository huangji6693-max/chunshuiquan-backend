package com.chunshuiquan.backend.repository;

import com.chunshuiquan.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {

    void deleteByReporterIdOrReportedId(UUID reporterId, UUID reportedId);
}
