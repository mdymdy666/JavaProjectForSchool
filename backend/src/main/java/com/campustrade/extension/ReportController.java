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
    public record HandleReportRequest(String status, String handlingResult, boolean offShelfProduct) {}

    @PostMapping("/reports")
    ApiResponse<Void> report(@RequestBody ReportRequest request,
            @AuthenticationPrincipal SecurityUser user) {
        reportService.report(user.userId(), request.productId, request.reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/reports/my")
    ApiResponse<List<ReportService.ReportView>> myReports(
            @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.success(reportService.reportsByReporter(user.userId()));
    }

    @GetMapping("/admin/reports")
    ApiResponse<List<ReportService.ReportView>> reports(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(reportService.reports(status));
    }

    @PostMapping("/admin/reports/{id}/resolve")
    ApiResponse<Void> resolveReport(@PathVariable long id,
            @AuthenticationPrincipal SecurityUser user) {
        reportService.resolve(id, user.userId());
        return ApiResponse.success(null);
    }

    @PostMapping("/admin/reports/{id}/handle")
    ApiResponse<Void> handleReport(@PathVariable long id,
            @RequestBody HandleReportRequest request,
            @AuthenticationPrincipal SecurityUser user) {
        reportService.handle(id, user.userId(), request.status(),
                request.handlingResult(), request.offShelfProduct());
        return ApiResponse.success(null);
    }
}
