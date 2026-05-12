package com.example.cacheconsistency.config;

import com.example.cacheconsistency.strategy.CacheStorage;
import com.example.cacheconsistency.strategy.InMemoryCacheStorage;
import com.example.cacheconsistency.strategy.RedisCacheStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConsistentAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheConsistentAutoConfiguration.class));

    @Test
    void shouldCreateRedisCacheStorageWhenRedisAvailable() {
        runner.withBean(StringRedisTemplate.class, () -> null)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheStorage.class);
                    assertThat(ctx.getBean(CacheStorage.class)).isInstanceOf(RedisCacheStorage.class);
                    assertThat(ctx).hasSingleBean(CacheConsistentProperties.class);
                    assertThat(ctx).hasSingleBean(com.example.cacheconsistency.aspect.CacheConsistentAspect.class);
                });
    }

    @Test
    void shouldFallbackToInMemoryWhenRedisNotAvailable() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(CacheStorage.class);
            assertThat(ctx.getBean(CacheStorage.class)).isInstanceOf(InMemoryCacheStorage.class);
            assertThat(ctx).hasSingleBean(com.example.cacheconsistency.aspect.CacheConsistentAspect.class);
        });
    }

    @Test
    void shouldRespectCustomCacheStorageBean() {
        runner.withBean(StringRedisTemplate.class, () -> null)
                .withBean(CacheStorage.class, () -> new CacheStorage() {
                    @Override
                    public void delete(String key) {
                    }
                    @Override
                    public void set(String key, String value, long ttl, TimeUnit unit) {
                    }
                })
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheStorage.class);
                    assertThat(ctx.getBean(CacheStorage.class)).isNotInstanceOf(RedisCacheStorage.class);
                    assertThat(ctx.getBean(CacheStorage.class)).isNotInstanceOf(InMemoryCacheStorage.class);
                });
    }

    @Test
    void shouldUseConfiguredKeyPrefix() {
        runner.withPropertyValues("cache-consistency.key-prefix=myapp:")
                .run(ctx -> {
                    CacheConsistentProperties props = ctx.getBean(CacheConsistentProperties.class);
                    assertThat(props.getKeyPrefix()).isEqualTo("myapp:");
                });
    }

    @Test
    void shouldUseConfiguredDefaultDelay() {
        runner.withPropertyValues("cache-consistency.default-delay-millis=1000")
                .run(ctx -> {
                    CacheConsistentProperties props = ctx.getBean(CacheConsistentProperties.class);
                    assertThat(props.getDefaultDelayMillis()).isEqualTo(1000);
                });
    }

    @Test
    void shouldUseDefaultsWhenNoConfig() {
        runner.run(ctx -> {
            CacheConsistentProperties props = ctx.getBean(CacheConsistentProperties.class);
            assertThat(props.getKeyPrefix()).isEqualTo("cache:");
            assertThat(props.getDefaultDelayMillis()).isEqualTo(500);
        });
    }
}
