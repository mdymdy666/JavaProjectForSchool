package com.campustrade.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.config.SecurityConfig;

@WebMvcTest(controllers = ErrorProbeController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsStructuredBusinessError() throws Exception {
        mockMvc.perform(get("/api/test-errors/product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("商品不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsFirstValidationMessage() throws Exception {
        mockMvc.perform(post("/api/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("name: 不能为空"));
    }
}

@RestController
@RequestMapping("/api/test-errors")
class ErrorProbeController {

    @GetMapping("/product")
    void missingProduct() {
        throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody ValidationRequest request) {
    }

    record ValidationRequest(@NotBlank(message = "不能为空") String name) {
    }
}
