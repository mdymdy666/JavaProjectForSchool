package com.campustrade.extension;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ReportView(long id, long reporterId, long productId, String reason,
            String reportStatus, LocalDateTime createdAt) {}

    @Transactional
    public void report(long reporterId, long productId, String reason) {
        jdbc.update("""
                INSERT INTO reports(reporter_id, product_id, reason, report_status)
                VALUES (?, ?, ?, 'PENDING')
                """, reporterId, productId, reason);
    }

    public List<ReportView> pendingReports() {
        return jdbc.query("""
                SELECT id, reporter_id, product_id, reason, report_status, created_at
                FROM reports WHERE report_status = 'PENDING' ORDER BY created_at DESC
                """, (rs, i) -> new ReportView(
                rs.getLong("id"), rs.getLong("reporter_id"),
                rs.getLong("product_id"), rs.getString("reason"),
                rs.getString("report_status"),
                rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @Transactional
    public void resolve(long reportId, long adminId) {
        jdbc.update("""
                UPDATE reports SET report_status = 'RESOLVED' WHERE id = ?
                """, reportId);
    }
}
