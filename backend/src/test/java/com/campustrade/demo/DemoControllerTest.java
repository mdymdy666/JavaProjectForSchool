package com.campustrade.demo;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providesDashboardProductsOrdersAndMessages() throws Exception {
        mockMvc.perform(get("/api/demo/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics", hasSize(4)));

        mockMvc.perform(get("/api/demo/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").exists());

        mockMvc.perform(get("/api/demo/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderNo").exists());

        mockMvc.perform(get("/api/demo/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").exists());
    }

    @Test
    void supportsAnswerDefenseTransactionFlow() throws Exception {
        mockMvc.perform(post("/api/demo/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "自行车车筐",
                                  "category": "生活用品",
                                  "seller": "演示卖家",
                                  "itemCondition": "八成新",
                                  "price": 18.00,
                                  "description": "适合校内自行车使用"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/demo/products/3/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true, \"reason\": \"信息完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/demo/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"buyer\": \"演示买家\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/demo/orders/301/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/demo/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sender": "演示买家",
                                  "receiver": "演示卖家",
                                  "productTitle": "九成新机械键盘",
                                  "content": "我想今天下午自提。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNREAD"));
    }
}
