package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.ReportRequest;
import com.chunshuiquan.backend.entity.Report;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.ReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;

    public ReportController(ReportRepository reportRepository,
                            ProfileRepository profileRepository) {
        this.reportRepository = reportRepository;
        this.profileRepository = profileRepository;
    }

    @PostMapping
    public ResponseEntity<Void> report(@AuthenticationPrincipal String userId,
                                       @RequestBody ReportRequest request) {
        UUID myId = UUID.fromString(userId);
        if (!profileRepository.existsById(request.getTargetId())) {
            return ResponseEntity.notFound().build();
        }
        Report report = new Report();
        report.setReporterId(myId);
        report.setReportedId(request.getTargetId());
        report.setReason(request.getReason());
        reportRepository.save(report);
        return ResponseEntity.ok().build();
    }
}
