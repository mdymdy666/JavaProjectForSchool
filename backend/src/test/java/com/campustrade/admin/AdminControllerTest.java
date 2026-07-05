package com.campustrade.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AuthMapper authMapper;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void protectsDashboardAndLetsAdminManageAnnouncement() throws Exception {
        UserAccount user = user("ordinary", "普通用户", "USER");
        UserAccount admin = user("dashboardAdmin", "后台管理员", "ADMIN");
        seedStatistics(user.getId());

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCount").value(2))
                .andExpect(jsonPath("$.data.productCount").value(1))
                .andExpect(jsonPath("$.data.orderCount").value(1))
                .andExpect(jsonPath("$.data.turnover").value(120.00))
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("图书教材"));

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"校园交易提醒","content":"请在公共场所完成线下交易","published":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));

        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("校园交易提醒"));

        mockMvc.perform(delete("/api/admin/announcements/1")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void seedStatistics(long sellerId) {
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('图书教材', 1, 1)");
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (?, 1, '软件工程教材', '正版', 120.00, '九成新', 'SOLD', 10, 1, 0)
                """, sellerId);
        jdbcTemplate.update("""
                INSERT INTO orders(order_no, buyer_id, seller_id, product_id, total_amount,
                    status, version, deleted)
                VALUES ('CT-DEMO-1', 2, ?, 1, 120.00, 'COMPLETED', 3, 0)
                """, sellerId);
    }

    private UserAccount user(String username, String nickname, String role) {
        UserAccount user = new UserAccount();
        user.setUsername(username); user.setPasswordHash("x"); user.setNickname(nickname);
        user.setRole(role); user.setStatus("NORMAL"); authMapper.insert(user);
        return user;
    }

    private String bearer(UserAccount user) {
        return "Bearer " + jwtService.create(user.getId(), user.getRole());
    }
}
