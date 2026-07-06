package com.campustrade.admin;

import static com.campustrade.admin.AdminDtos.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;

@Service
public class AdminService {
    private final AdminMapper adminMapper;
    private final AnnouncementMapper announcementMapper;
    private final AuthMapper authMapper;
    private final JdbcTemplate jdbc;

    public AdminService(AdminMapper adminMapper, AnnouncementMapper announcementMapper,
            AuthMapper authMapper, JdbcTemplate jdbc) {
        this.adminMapper = adminMapper;
        this.announcementMapper = announcementMapper;
        this.authMapper = authMapper;
        this.jdbc = jdbc;
    }

    public DashboardView dashboard() {
        return new DashboardView(adminMapper.userCount(), adminMapper.productCount(),
                adminMapper.orderCount(), adminMapper.turnover(), adminMapper.categoryStats());
    }

    @Transactional
    public AnnouncementView create(AnnouncementRequest request) {
        Announcement a = new Announcement();
        a.setTitle(request.title().trim());
        a.setContent(request.content().trim());
        a.setPublished(request.published() ? 1 : 0);
        announcementMapper.insert(a);
        return view(a);
    }

    @Transactional
    public void delete(long id) { announcementMapper.deleteById(id); }

    public List<AnnouncementView> published() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getPublished, 1).orderByDesc(Announcement::getCreatedAt))
                .stream().map(this::view).toList();
    }

    // ---- User Management ----

    public record UserRow(long id, String username, String nickname, String phone,
            String email, String role, String status, LocalDateTime createdAt) {}

    public record UserPage(List<UserRow> records, long total) {}

    public UserPage listUsers(String keyword, int page, int size) {
        String where = "";
        if (keyword != null && !keyword.isBlank()) {
            where = " WHERE u.username LIKE '%" + keyword.replace("'", "''") + "%' "
                    + "OR u.nickname LIKE '%" + keyword.replace("'", "''") + "%'";
        }
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users u" + where, Long.class);
        int offset = (page - 1) * size;
        List<UserRow> rows = jdbc.query(
                "SELECT u.id, u.username, u.nickname, u.phone, u.email, u.role, u.status, u.created_at "
                        + "FROM users u" + where + " ORDER BY u.id ASC LIMIT ? OFFSET ?",
                (rs, i) -> new UserRow(
                        rs.getLong("id"), rs.getString("username"), rs.getString("nickname"),
                        rs.getString("phone"), rs.getString("email"), rs.getString("role"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                size, offset);
        return new UserPage(rows, total);
    }

    @Transactional
    public UserRow toggleUserStatus(long userId) {
        UserAccount user = authMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if ("ADMIN".equals(user.getRole())) throw new BusinessException(ErrorCode.FORBIDDEN, "不能禁用管理员");
        String newStatus = "NORMAL".equals(user.getStatus()) ? "DISABLED" : "NORMAL";
        user.setStatus(newStatus);
        authMapper.updateById(user);
        return new UserRow(user.getId(), user.getUsername(), user.getNickname(),
                user.getPhone(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    // ---- Order Management ----

    public record AdminOrderRow(long id, String orderNo, String productTitle,
            String buyerNickname, String sellerNickname, BigDecimal totalAmount,
            String status, String logisticsInfo, LocalDateTime createdAt) {}

    public record AdminOrderPage(List<AdminOrderRow> records, long total) {}

    public AdminOrderPage listAllOrders(String status, int page, int size) {
        StringBuilder where = new StringBuilder();
        if (status != null && !status.isBlank()) {
            where.append(" WHERE o.status = '").append(status.replace("'", "''")).append("'");
        }
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders o" + where, Long.class);
        int offset = (page - 1) * size;
        List<AdminOrderRow> rows = jdbc.query(
                "SELECT o.id, o.order_no, p.title AS product_title, "
                        + "ub.nickname AS buyer_nickname, us.nickname AS seller_nickname, "
                        + "o.total_amount, o.status, o.logistics_info, o.created_at "
                        + "FROM orders o "
                        + "JOIN products p ON p.id = o.product_id "
                        + "JOIN users ub ON ub.id = o.buyer_id "
                        + "JOIN users us ON us.id = o.seller_id "
                        + where + " ORDER BY o.created_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> new AdminOrderRow(
                        rs.getLong("id"), rs.getString("order_no"),
                        rs.getString("product_title"), rs.getString("buyer_nickname"),
                        rs.getString("seller_nickname"), rs.getBigDecimal("total_amount"),
                        rs.getString("status"), rs.getString("logistics_info"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                size, offset);
        return new AdminOrderPage(rows, total);
    }

    // ---- Trend Data ----

    public record TrendPoint(String date, long orderCount, BigDecimal amount) {}

    public List<TrendPoint> weekTrend() {
        LocalDate today = LocalDate.now();
        List<LocalDate> days = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) days.add(today.minusDays(i));

        StringBuilder sql = new StringBuilder(
                "SELECT d.dt AS date, COALESCE(COUNT(o.id), 0) AS order_count, "
                        + "COALESCE(SUM(o.total_amount), 0) AS amount FROM (");
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) sql.append(" UNION ALL ");
            sql.append("SELECT CAST(? AS DATE) AS dt");
        }
        sql.append(") d LEFT JOIN orders o ON CAST(o.created_at AS DATE) = d.dt AND o.deleted = 0 "
                + "GROUP BY d.dt ORDER BY d.dt ASC");

        Object[] params = days.toArray();
        return jdbc.query(sql.toString(),
                (rs, i) -> new TrendPoint(
                        rs.getDate("date").toLocalDate().toString(),
                        rs.getLong("order_count"),
                        rs.getBigDecimal("amount")),
                params);
    }

    private AnnouncementView view(Announcement a) {
        return new AnnouncementView(a.getId(), a.getTitle(), a.getContent(),
                Integer.valueOf(1).equals(a.getPublished()), a.getCreatedAt());
    }
}
