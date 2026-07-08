package com.campustrade.message;

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
import org.springframework.test.web.servlet.MvcResult;

import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MessageControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AuthMapper authMapper;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void sendsListsAndReadsProductMessageAndNotification() throws Exception {
        UserAccount seller = user("msgSeller", "留言卖家");
        UserAccount buyer = user("msgBuyer", "留言买家");
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('数码', 1, 1)");
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (?, 1, '校园耳机', '配件齐全', 88.00, '九成新', 'APPROVED', 0, 0, 0)
                """, seller.getId());

        MvcResult sent = mockMvc.perform(post("/api/messages")
                        .header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverId":1,"productId":1,"content":"今天可以在图书馆自提吗？"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readStatus").value("UNREAD"))
                .andExpect(jsonPath("$.data.productTitle").value("校园耳机"))
                .andExpect(jsonPath("$.data.senderNickname").value("留言买家"))
                .andReturn();
        long messageId = objectMapper.readTree(sent.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/messages").header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("今天可以在图书馆自提吗？"))
                .andExpect(jsonPath("$.data[0].readStatus").value("UNREAD"));

        mockMvc.perform(post("/api/messages/{id}/read", messageId)
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readStatus").value("READ"));

        jdbcTemplate.update("""
                INSERT INTO orders(order_no, buyer_id, seller_id, product_id, total_amount,
                    status, version, deleted)
                VALUES ('CT-MESSAGE-1', ?, ?, 1, 88.00, 'PAID', 1, 0)
                """, buyer.getId(), seller.getId());
        mockMvc.perform(post("/api/messages")
                        .header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiverId":2,"productId":1,"content":"可以，下午三点见。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverId").value(buyer.getId()));

        jdbcTemplate.update("""
                INSERT INTO notifications(user_id, type, title, content, read_status)
                VALUES (?, 'ORDER_STATUS', '订单状态更新', '订单已发货', 'UNREAD')
                """, buyer.getId());
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].readStatus").value("UNREAD"));
        mockMvc.perform(post("/api/notifications/1/read").header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readStatus").value("READ"));
    }

    private UserAccount user(String username, String nickname) {
        UserAccount user = new UserAccount();
        user.setUsername(username); user.setPasswordHash("x"); user.setNickname(nickname);
        user.setRole("USER"); user.setStatus("NORMAL"); authMapper.insert(user);
        return user;
    }

    private String bearer(UserAccount user) {
        return "Bearer " + jwtService.create(user.getId(), user.getRole());
    }
}
