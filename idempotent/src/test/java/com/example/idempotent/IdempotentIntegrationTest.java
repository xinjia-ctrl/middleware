package com.example.idempotent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@AutoConfigureMockMvc
class IdempotentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void firstRequestShouldSucceed() throws Exception {
        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "order-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void duplicateRequestShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "order-002"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "order-002"))
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("重复请求，请勿重复提交"));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() throws Exception {
        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "key-a"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "key-b"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/dedup")
                        .header("Idempotent-Key", "key-a"))
                .andExpect(status().is(409));
    }

    @Test
    void cacheResultShouldReturnCachedValueOnDuplicate() throws Exception {
        mockMvc.perform(get("/test/cache-result")
                        .header("Idempotent-Key", "cache-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("第一次结果"));

        mockMvc.perform(get("/test/cache-result")
                        .header("Idempotent-Key", "cache-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("第一次结果"));
    }

    @Test
    void businessExceptionShouldAllowRetry() throws Exception {
        assertThrows(Exception.class, () ->
            mockMvc.perform(get("/test/retry-on-error")
                    .header("Idempotent-Key", "retry-001"))
        );

        mockMvc.perform(get("/test/retry-on-error")
                        .header("Idempotent-Key", "retry-001"))
                .andExpect(status().isOk())
                .andExpect(content().string("重试成功"));
    }
}
