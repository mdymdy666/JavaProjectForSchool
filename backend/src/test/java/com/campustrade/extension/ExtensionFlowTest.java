package com.campustrade.extension;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ExtensionFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('数码配件', 1, 1)");
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Pass1234\",\"nickname\":\"测试\"}"))
                .andExpect(status().isOk());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + username + "\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String registerAdminAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Pass1234\",\"nickname\":\"管理员\"}"))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", username);
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"" + username + "\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    void sensitiveWordProductIsRejected() throws Exception {
        String token = registerAndLogin("sw-seller");

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"卖违禁品","categoryId":1,"price":10,
                        "itemCondition":"全新","description":"test","imageUrls":["http://a.jpg"]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("内容包含敏感词，请修改后重试"));
    }

    @Test
    void sensitiveWordInDescriptionIsRejected() throws Exception {
        String token = registerAndLogin("sw-seller2");

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"正常商品","categoryId":1,"price":10,
                        "itemCondition":"全新","description":"这是毒品","imageUrls":["http://a.jpg"]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("内容包含敏感词，请修改后重试"));
    }

    @Test
    void recommendEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/products/recommend").param("limit", "4"))
                .andExpect(status().isOk());
    }

    @Test
    void personalizedRecommendUsesUserBehavior() throws Exception {
        String token = registerAndLogin("rec-user");
        Long buyerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'rec-user'", Long.class);
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('图书教材', 2, 1)");
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('rec-seller', 'x', '推荐卖家', 'USER', 'NORMAL')
                """);
        Long sellerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'rec-seller'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, deleted)
                VALUES (?, 1, '蓝牙机械键盘', '适合写代码', 99, '九成新', 'APPROVED', 10, 0)
                """, sellerId);
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, deleted)
                VALUES (?, 2, '考研数学真题', '图书教材', 35, '轻微笔记', 'APPROVED', 300, 0)
                """, sellerId);
        Long keyboardId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE title = '蓝牙机械键盘'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO user_behaviors(user_id, product_id, behavior_type, keyword, category_id, weight)
                VALUES (?, ?, 'SEARCH', '键盘', 1, 30)
                """, buyerId, keyboardId);

        mockMvc.perform(get("/api/products/recommend").param("limit", "1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("蓝牙机械键盘"))
                .andExpect(jsonPath("$.data[0].reason").value("匹配你搜索过的“键盘”"));
    }

    @Test
    void productSearchRecordsBehaviorForLoggedInUser() throws Exception {
        String token = registerAndLogin("search-rec-user");

        mockMvc.perform(get("/api/products").param("keyword", "键盘")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM user_behaviors ub
                JOIN users u ON u.id = ub.user_id
                WHERE u.username = 'search-rec-user'
                  AND ub.behavior_type = 'SEARCH'
                  AND ub.keyword = '键盘'
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    void userCanReportProduct() throws Exception {
        String sellerToken = registerAndLogin("rep-seller");
        String reporterToken = registerAndLogin("rep-user");

        MvcResult pub = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"测试商品","categoryId":1,"price":50,
                        "itemCondition":"全新","description":"正常描述","imageUrls":["http://b.jpg"]}
                        """))
                .andExpect(status().isOk()).andReturn();
        long productId = objectMapper.readTree(pub.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/reports")
                .header("Authorization", "Bearer " + reporterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"reason\":\"虚假描述\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/my")
                .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productTitle").value("测试商品"))
                .andExpect(jsonPath("$.data[0].reportStatus").value("PENDING"));
    }

    @Test
    void adminCanMaintainSensitiveWords() throws Exception {
        String adminToken = registerAdminAndLogin("word-admin");

        mockMvc.perform(post("/api/admin/sensitive-words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word\":\"私下转账\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.word").value("私下转账"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        String sellerToken = registerAndLogin("word-seller");
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + sellerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"正常标题","categoryId":1,"price":10,
                        "itemCondition":"全新","description":"请私下转账","imageUrls":["http://a.jpg"]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("内容包含敏感词，请修改后重试"));
    }

    @Test
    void adminCanHandleReportAndOffShelfProduct() throws Exception {
        String reporterToken = registerAndLogin("reporter-user");
        String adminToken = registerAdminAndLogin("report-admin");
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('report-seller', 'x', '举报卖家', 'USER', 'NORMAL')
                """);
        Long sellerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'report-seller'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, deleted)
                VALUES (?, 1, '争议商品', '描述不清', 99, '九成新', 'APPROVED', 10, 0)
                """, sellerId);
        Long productId = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE title = '争议商品'", Long.class);

        mockMvc.perform(post("/api/reports")
                .header("Authorization", "Bearer " + reporterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"reason\":\"疑似虚假描述\"}"))
                .andExpect(status().isOk());
        Long reportId = jdbcTemplate.queryForObject("SELECT id FROM reports", Long.class);

        mockMvc.perform(post("/api/admin/reports/{id}/handle", reportId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PRODUCT_REMOVED","handlingResult":"举报属实，商品已下架","offShelfProduct":true}
                        """))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT report_status FROM reports WHERE id = ?", String.class, reportId))
                .isEqualTo("PRODUCT_REMOVED");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM products WHERE id = ?", String.class, productId))
                .isEqualTo("OFF_SHELF");
    }
}
