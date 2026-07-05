package com.campustrade.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.campustrade.config.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import({ProductService.class, SecurityConfig.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
