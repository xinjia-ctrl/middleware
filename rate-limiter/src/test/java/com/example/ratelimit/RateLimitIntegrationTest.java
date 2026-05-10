package com.example.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenBucketShouldRejectWhenExceeded() throws Exception {
        // capacity=2, first 2 should succeed
        mockMvc.perform(get("/token-bucket")).andExpect(status().isOk());
        mockMvc.perform(get("/token-bucket")).andExpect(status().isOk());
        // 3rd → 429
        mockMvc.perform(get("/token-bucket"))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value(429));
    }

    @Test
    void silentRejectShouldReturnEmptyBody() throws Exception {
        mockMvc.perform(get("/reject-silent")).andExpect(status().isOk());
        mockMvc.perform(get("/reject-silent")).andExpect(content().string(""));
    }

    @Test
    void fallbackShouldReturnCustomMessage() throws Exception {
        mockMvc.perform(get("/reject-fallback")).andExpect(status().isOk())
                .andExpect(content().string("REJECT_FALLBACK: OK"));
        mockMvc.perform(get("/reject-fallback")).andExpect(status().isOk())
                .andExpect(content().string("当前请求被限流，返回降级结果"));
    }
}
