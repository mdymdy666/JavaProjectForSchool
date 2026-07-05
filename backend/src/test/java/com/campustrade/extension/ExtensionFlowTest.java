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
    void userCanReportProduct() throws Exception {
        String token = registerAndLogin("rep-user");

        MvcResult pub = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"测试商品","categoryId":1,"price":50,
                        "itemCondition":"全新","description":"正常描述","imageUrls":["http://b.jpg"]}
                        """))
                .andExpect(status().isOk()).andReturn();
        long productId = objectMapper.readTree(pub.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/reports")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + productId + ",\"reason\":\"虚假描述\"}"))
                .andExpect(status().isOk());
    }
}
