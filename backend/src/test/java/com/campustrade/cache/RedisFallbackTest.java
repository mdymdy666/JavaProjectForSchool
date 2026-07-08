package com.campustrade.cache;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RedisFallbackTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void servesProductAndCaptchaWhenRedisIsDisabled() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('fallback-seller', 'x', '降级卖家', 'USER', 'NORMAL')
                """);
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('降级分类', 1, 1)");
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (1, 1, '离线可见商品', 'Redis 关闭仍可访问', 30.00,
                    '九成新', 'APPROVED', 7, 0, 0)
                """);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("离线可见商品"));

        mockMvc.perform(post("/api/auth/captcha/register").param("account", "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("123456"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
    }

    @Test
    void loginWorksWhenRedisIsDisabled() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('rate-user', ?, '限流测试', 'USER', 'NORMAL')
                """, passwordEncoder.encode("correct-pw"));

        // Login with correct password should succeed even without Redis
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"rate-user","password":"correct-pw"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Login with wrong password should return 401 (not rate-limited since Redis disabled)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"rate-user","password":"wrong-pw"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWorksWhenRedisIsDisabled() throws Exception {
        String pw = passwordEncoder.encode("logout-pw");
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('logout-user', ?, '登出测试', 'USER', 'NORMAL')
                """, pw);

        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"logout-user\",\"password\":\"logout-pw\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Logout should succeed even when Redis is disabled (blacklist write skipped)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
