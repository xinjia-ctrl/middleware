package com.example.cacheconsistency;

import com.example.cacheconsistency.config.CacheConsistentAutoConfiguration;
import com.example.cacheconsistency.config.CacheConsistentProperties;
import com.example.cacheconsistency.strategy.CacheStorage;
import com.example.cacheconsistency.strategy.InMemoryCacheStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CacheConsistentIntegrationTest {

    @Autowired(required = false)
    private CacheStorage cacheStorage;

    @Autowired(required = false)
    private CacheConsistentProperties properties;

    @Test
    void contextShouldLoadWithoutRedis() {
        assertThat(cacheStorage).isNotNull();
        assertThat(properties).isNotNull();
    }

    @Test
    void shouldUseInMemoryFallback() {
        assertThat(cacheStorage).isInstanceOf(InMemoryCacheStorage.class);
    }

    @Test
    void shouldHaveDefaultProperties() {
        assertThat(properties.getKeyPrefix()).isEqualTo("cache:");
        assertThat(properties.getDefaultDelayMillis()).isEqualTo(500);
    }

    @Test
    void shouldStoreAndDeleteValue() {
        cacheStorage.set("test-key", "test-value", 10, TimeUnit.SECONDS);
        cacheStorage.delete("test-key");
        // no exception thrown - success
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CacheConsistentAutoConfiguration.class)
    static class TestConfig {
    }
}
