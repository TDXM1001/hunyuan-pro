package com.hunyuan.sa.admin.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class IsolatedInfrastructureTestSupport {

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironment("HUNYUAN_IT_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironment("HUNYUAN_IT_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", () -> environment("HUNYUAN_IT_REDIS_HOST", "127.0.0.1"));
        registry.add("spring.data.redis.port", () -> Integer.parseInt(environment("HUNYUAN_IT_REDIS_PORT", "6379")));
        registry.add("spring.data.redis.database", () -> Integer.parseInt(environment("HUNYUAN_IT_REDIS_DATABASE", "15")));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.placeholder-replacement", () -> false);
        registry.add("smart.job.enabled", () -> false);
        registry.add("reload.interval-seconds", () -> 3600);
    }

    protected static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for isolated integration tests");
        }
        return value;
    }

    protected static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
