package com.campustrade.extension;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.message.NotificationService;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public ReportService(JdbcTemplate jdbc, NotificationService notificationService) {
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    public record ReportView(long id, long reporterId, String reporterNickname,
            long productId, String productTitle, String sellerNickname, String reason,
            String reportStatus, String handlingResult, LocalDateTime createdAt,
            LocalDateTime processedAt) {}

    @Transactional
    public void report(long reporterId, long productId, String reason) {
        String cleanReason = normalizeReason(reason);
        ProductOwner product = productOwner(productId);
        if (product == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        if (product.sellerId == reporterId) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能举报自己的商品");
        }
        jdbc.update("""
                INSERT INTO reports(reporter_id, product_id, reason, report_status)
                VALUES (?, ?, ?, 'PENDING')
                """, reporterId, productId, cleanReason);
        notificationService.create(product.sellerId, "REPORT", "商品收到举报",
                "商品《" + product.title + "》收到一条举报，平台将尽快处理");
    }

    public List<ReportView> reports(String status) {
        String where = StringUtils.hasText(status)
                ? " WHERE r.report_status = '" + status.replace("'", "''") + "' "
                : "";
        return jdbc.query("""
                SELECT r.id, r.reporter_id, ur.nickname AS reporter_nickname,
                       r.product_id, p.title AS product_title, us.nickname AS seller_nickname,
                       r.reason, r.report_status, r.handling_result,
                       r.created_at, r.processed_at
                FROM reports r
                JOIN users ur ON ur.id = r.reporter_id
                JOIN products p ON p.id = r.product_id
                JOIN users us ON us.id = p.seller_id
                """ + where + """
                ORDER BY r.created_at DESC
                """, (rs, i) -> new ReportView(
                rs.getLong("id"),
                rs.getLong("reporter_id"),
                rs.getString("reporter_nickname"),
                rs.getLong("product_id"),
                rs.getString("product_title"),
                rs.getString("seller_nickname"),
                rs.getString("reason"),
                rs.getString("report_status"),
                rs.getString("handling_result"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toLocalDateTime()));
    }

    public List<ReportView> pendingReports() {
        return reports("PENDING");
    }

    @Transactional
    public void handle(long reportId, long adminId, String status, String handlingResult, boolean offShelfProduct) {
        String finalStatus = switch (status == null ? "RESOLVED" : status) {
            case "REJECTED" -> "REJECTED";
            case "PRODUCT_REMOVED" -> "PRODUCT_REMOVED";
            default -> "RESOLVED";
        };
        ReportTarget target = reportTarget(reportId);
        if (target == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "举报不存在");
        String result = StringUtils.hasText(handlingResult) ? handlingResult.trim() : defaultResult(finalStatus);

        jdbc.update("""
                UPDATE reports
                SET report_status = ?, admin_id = ?, handling_result = ?, processed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, finalStatus, adminId, result, reportId);

        if (offShelfProduct || "PRODUCT_REMOVED".equals(finalStatus)) {
            jdbc.update("""
                    UPDATE products
                    SET status = 'OFF_SHELF', version = version + 1
                    WHERE id = ? AND status = 'APPROVED' AND deleted = 0
                    """, target.productId());
        }

        notificationService.create(target.reporterId(), "REPORT_RESULT", "举报处理结果", result);
        notificationService.create(target.sellerId(), "REPORT_RESULT", "商品举报处理结果",
                "商品《" + target.productTitle() + "》举报处理结果：" + result);
    }

    @Transactional
    public void resolve(long reportId, long adminId) {
        handle(reportId, adminId, "RESOLVED", "举报已处理", false);
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "举报原因不能为空");
        }
        String clean = reason.trim();
        if (clean.length() > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "举报原因不能超过 500 字");
        }
        return clean;
    }

    private String defaultResult(String status) {
        return switch (status) {
            case "REJECTED" -> "举报依据不足，已驳回";
            case "PRODUCT_REMOVED" -> "举报属实，商品已下架";
            default -> "举报已处理";
        };
    }

    private ProductOwner productOwner(long productId) {
        List<ProductOwner> rows = jdbc.query("""
                SELECT id, seller_id, title FROM products WHERE id = ? AND deleted = 0
                """, (rs, i) -> new ProductOwner(
                rs.getLong("id"), rs.getLong("seller_id"), rs.getString("title")), productId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ReportTarget reportTarget(long reportId) {
        List<ReportTarget> rows = jdbc.query("""
                SELECT r.id, r.reporter_id, r.product_id, p.seller_id, p.title AS product_title
                FROM reports r
                JOIN products p ON p.id = r.product_id
                WHERE r.id = ?
                """, (rs, i) -> new ReportTarget(
                rs.getLong("id"), rs.getLong("reporter_id"), rs.getLong("product_id"),
                rs.getLong("seller_id"), rs.getString("product_title")), reportId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private record ProductOwner(long productId, long sellerId, String title) {}
    private record ReportTarget(long id, long reporterId, long productId, long sellerId, String productTitle) {}
}
