package com.chunshuiquan.backend.dto;

import com.chunshuiquan.backend.entity.Report;
import lombok.Data;

import java.util.UUID;

@Data
public class ReportRequest {
    private UUID targetId;
    private Report.Reason reason;
}
