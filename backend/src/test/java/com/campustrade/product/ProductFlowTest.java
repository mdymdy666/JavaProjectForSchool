package com.campustrade.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductFlowTest {

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
    void completesProductPublishAuditSearchFavoriteAndOffShelfFlow() throws Exception {
        UserAccount seller = createUser("seller01", "校园卖家", "USER");
        UserAccount buyer = createUser("buyer01", "校园买家", "USER");
        UserAccount admin = createUser("admin01", "审核员", "ADMIN");
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES (?, ?, ?)",
                "数码产品", 1, 1);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name = ?", Long.class, "数码产品");

        MvcResult publishResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId":%d,
                                  "title":"蓝牙降噪耳机",
                                  "description":"自习室使用安静舒适，配件齐全",
                                  "price":129.00,
                                  "itemCondition":"九成新",
                                  "imageUrls":["/uploads/headset-1.jpg","/uploads/headset-2.jpg"]
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andReturn();

        JsonNode publishBody = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        long productId = publishBody.path("data").path("id").asLong();

        mockMvc.perform(get("/api/products").param("keyword", "降噪"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/admin/products/{id}/audit", productId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approved":true,"reason":"信息完整"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/products")
                        .param("keyword", "降噪")
                        .param("sort", "priceAsc")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("蓝牙降噪耳机"));

        mockMvc.perform(post("/api/products/{id}/favorite", productId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(post("/api/products/{id}/favorite", productId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", bearer(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true))
                .andExpect(jsonPath("$.data.sellerNickname").value("校园卖家"));

        mockMvc.perform(post("/api/products/{id}/off-shelf", productId)
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFF_SHELF"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?",
                Integer.class, buyer.getId(), productId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'PRODUCT' AND target_id = ?",
                Integer.class, productId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'PRODUCT_AUDIT'",
                Integer.class, seller.getId())).isEqualTo(1);
    }

    @Test
    void letsOwnerEditRelistAndSoftDeleteProduct() throws Exception {
        UserAccount seller = createUser("seller02", "第二卖家", "USER");
        UserAccount stranger = createUser("stranger", "其他用户", "USER");
        UserAccount admin = createUser("admin02", "审核员", "ADMIN");
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES (?, ?, ?)",
                "图书教材", 1, 1);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name = ?", Long.class, "图书教材");

        long productId = publish(seller, categoryId, "考研数学教材");

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "越权修改")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, "考研数学教材全套")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("考研数学教材全套"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/admin/products/{id}/audit", productId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"reason\":\"信息完整\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products/{id}/off-shelf", productId)
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products/{id}/on-shelf", productId)
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", bearer(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted FROM products WHERE id = ?", Integer.class, productId)).isEqualTo(1);
    }

    private long publish(UserAccount seller, long categoryId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(seller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(categoryId, title)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private String productJson(long categoryId, String title) {
        return """
                {
                  "categoryId":%d,
                  "title":"%s",
                  "description":"正版教材，内页干净",
                  "price":45.00,
                  "itemCondition":"八成新",
                  "imageUrls":["/uploads/book.jpg"]
                }
                """.formatted(categoryId, title);
    }

    private UserAccount createUser(String username, String nickname, String role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash("not-used-in-this-flow");
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus("NORMAL");
        authMapper.insert(user);
        return user;
    }

    private String bearer(UserAccount user) {
        return "Bearer " + jwtService.create(user.getId(), user.getRole());
    }
}
