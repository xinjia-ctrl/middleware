package com.example.ratelimit;

import com.example.ratelimit.annotation.RateLimit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ClassLevelRateLimitIntegrationTest.ClassLevelController.class)
class ClassLevelRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void classLevelAnnotationShouldLimitMethods() throws Exception {
        mockMvc.perform(get("/class-level")).andExpect(status().isOk());
        mockMvc.perform(get("/class-level")).andExpect(status().isTooManyRequests());
    }

    @RestController
    @RequestMapping("/class-level")
    @RateLimit(permitsPerSecond = 1, capacity = 1, key = "class-level-test")
    static class ClassLevelController {

        @GetMapping
        String limited() {
            return "OK";
        }
    }
}
