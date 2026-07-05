package com.campustrade.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;

import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderFlowTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completesPaymentShippingAndReceiptStateMachine() throws Exception {
        UserAccount seller = createUser("orderSeller", "订单卖家");
        UserAccount buyer = createUser("orderBuyer", "订单买家");
        long productId = createApprovedProduct(seller.getId(), "宿舍台灯", "68.00");

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(68.00))
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/orders").param("role", "buyer")
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(orderId));
        mockMvc.perform(get("/api/orders").param("role", "seller")
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(orderId));

        mockMvc.perform(post("/api/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));

        mockMvc.perform(post("/api/orders/{id}/pay", orderId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                        .header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsInfo\":\"校内配送-东门自提柜 A12\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/{id}/confirm", orderId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_logs WHERE order_id = ?", Integer.class, orderId))
                .isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM products WHERE id = ?", String.class, productId))
                .isEqualTo("SOLD");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE type = 'ORDER_STATUS'", Integer.class))
                .isEqualTo(4);
    }

    @Test
    void restoresProductWhenBuyerCancelsPendingOrder() throws Exception {
        UserAccount seller = createUser("cancelSeller", "取消卖家");
        UserAccount buyer = createUser("cancelBuyer", "取消买家");
        long productId = createApprovedProduct(seller.getId(), "折叠椅", "25.00");
        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long orderId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM products WHERE id = ?", String.class, productId))
                .isEqualTo("APPROVED");
    }

    private UserAccount createUser(String username, String nickname) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash("unused");
        user.setNickname(nickname);
        user.setRole("USER");
        user.setStatus("NORMAL");
        authMapper.insert(user);
        return user;
    }

    private long createApprovedProduct(long sellerId, String title, String price) {
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('生活用品', 1, 1)");
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name = '生活用品'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO products(
                    seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (?, ?, ?, '功能正常', ?, '九成新', 'APPROVED', 0, 0, 0)
                """, sellerId, categoryId, title, price);
        return jdbcTemplate.queryForObject("SELECT id FROM products WHERE title = ?", Long.class, title);
    }

    private String bearer(UserAccount user) {
        return "Bearer " + jwtService.create(user.getId(), user.getRole());
    }
}
