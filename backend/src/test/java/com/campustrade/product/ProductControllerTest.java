package com.campustrade.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedFeaturedProduct() {
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status)
                VALUES ('seller', 'unused', '计科小李', 'USER', 'NORMAL')
                """);
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('数码配件', 1, 1)");
        jdbcTemplate.update("""
                INSERT INTO products(
                    seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (1, 1, '九成新机械键盘', '配件齐全', 129.00,
                    '九成新', 'APPROVED', 88, 0, 0)
                """);
        jdbcTemplate.update("""
                INSERT INTO product_images(product_id, image_url, sort_order)
                VALUES (1, '/uploads/keyboard.jpg', 0)
                """);
    }

    @Test
    void returnsFeaturedProductsForHomepageDemo() throws Exception {
        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("九成新机械键盘"))
                .andExpect(jsonPath("$.data[0].price").value(129.00))
                .andExpect(jsonPath("$.data[0].category").value("数码配件"));
    }
}
