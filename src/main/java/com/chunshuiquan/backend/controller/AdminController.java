package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.ResolveReportRequest;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.Report;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.ReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ReportRepository reportRepository;
    private final ProfileRepository profileRepository;

    public AdminController(ReportRepository reportRepository,
                           ProfileRepository profileRepository) {
        this.reportRepository = reportRepository;
        this.profileRepository = profileRepository;
    }

    /** GET /api/admin/reports — 获取待处理举报列表 */
    @GetMapping("/reports")
    public ResponseEntity<List<Report>> pendingReports() {
        return ResponseEntity.ok(reportRepository.findByStatusOrderByCreatedAtAsc("pending"));
    }

    /** GET /api/admin/reports/all — 获取全部举报（含已处理） */
    @GetMapping("/reports/all")
    public ResponseEntity<List<Report>> allReports() {
        return ResponseEntity.ok(reportRepository.findAllByOrderByCreatedAtDesc());
    }

    /** PUT /api/admin/reports/{id}/resolve — 处理举报 */
    @PutMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Long id,
                                           @RequestBody ResolveReportRequest request) {
        String action = request.getAction();
        if (action == null || (!action.equals("warn") && !action.equals("ban") && !action.equals("dismiss"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "action 必须为 warn / ban / dismiss"));
        }

        return reportRepository.findById(id)
                .map(report -> {
                    report.setStatus("resolved");
                    report.setResolution(action);
                    reportRepository.save(report);

                    // ban: 封禁被举报用户
                    if ("ban".equals(action)) {
                        profileRepository.findById(report.getReportedId()).ifPresent(profile -> {
                            profile.setIsActive(false);
                            profileRepository.save(profile);
                        });
                    }

                    return ResponseEntity.ok(report);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/admin/users/{userId} — 查看用户详情（管理用途） */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Profile> getUser(@PathVariable java.util.UUID userId) {
        return profileRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/admin/users/{userId}/ban — 直接封禁用户 */
    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<?> banUser(@PathVariable java.util.UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> {
                    profile.setIsActive(false);
                    profileRepository.save(profile);
                    return ResponseEntity.ok(Map.of("message", "用户已封禁"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/admin/users/{userId}/unban — 解封用户 */
    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable java.util.UUID userId) {
        return profileRepository.findById(userId)
                .map(profile -> {
                    profile.setIsActive(true);
                    profileRepository.save(profile);
                    return ResponseEntity.ok(Map.of("message", "用户已解封"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
