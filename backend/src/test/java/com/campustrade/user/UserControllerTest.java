package com.campustrade.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updatesProfileAvatarAddressAndVerification() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname":"Demo Student",
                                  "phone":"13800001111",
                                  "email":"demo@campus.edu",
                                  "avatarUrl":"/uploads/avatar-demo.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Demo Student"))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatar-demo.png"))
                .andExpect(jsonPath("$.data.creditScore").value(95))
                .andExpect(jsonPath("$.data.realNameStatus").value("UNVERIFIED"));

        MvcResult created = mockMvc.perform(post("/api/users/me/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName":"Demo Student",
                                  "receiverPhone":"13800001111",
                                  "detailAddress":"Library north gate",
                                  "defaultAddress":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receiverName").value("Demo Student"))
                .andExpect(jsonPath("$.data[0].defaultAddress").value(true))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        long addressId = body.path("data").path(0).path("id").asLong();

        mockMvc.perform(post("/api/users/me/addresses/" + addressId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].defaultAddress").value(true));

        mockMvc.perform(post("/api/users/me/verification")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "realName":"Li Demo",
                                  "idCardNo":"110101200001011234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("Li Demo"))
                .andExpect(jsonPath("$.data.realNameStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.creditScore").value(98));

        mockMvc.perform(delete("/api/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"profile01",
                                  "password":"Pass1234",
                                  "nickname":"Profile User"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"profile01","password":"Pass1234"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
