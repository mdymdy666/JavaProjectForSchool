package com.campustrade.extension;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.campustrade.common.ApiResponse;
import com.campustrade.security.SecurityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public record ReportRequest(long productId, String reason) {}

    @PostMapping("/reports")
    ApiResponse<Void> report(@RequestBody ReportRequest request,
            @AuthenticationPrincipal SecurityUser user) {
        reportService.report(user.userId(), request.productId, request.reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/admin/reports")
    ApiResponse<List<ReportService.ReportView>> pendingReports() {
        return ApiResponse.success(reportService.pendingReports());
    }

    @PostMapping("/admin/reports/{id}/resolve")
    ApiResponse<Void> resolveReport(@PathVariable long id,
            @AuthenticationPrincipal SecurityUser user) {
        reportService.resolve(id, user.userId());
        return ApiResponse.success(null);
    }
}
