package com.hunyuan.sa.admin.integration;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "HUNYUAN_IT_ENABLED", matches = "(?i)true")
class RedisIsolationTest extends IsolatedInfrastructureTestSupport {

    @Test
    void dedicatedRedisDatabaseShouldBeReachableAndWritable() {
        String host = environment("HUNYUAN_IT_REDIS_HOST", "127.0.0.1");
        int port = Integer.parseInt(environment("HUNYUAN_IT_REDIS_PORT", "6379"));
        int database = Integer.parseInt(environment("HUNYUAN_IT_REDIS_DATABASE", "15"));
        assertThat(database).isGreaterThan(0);

        RedisURI redisUri = RedisURI.Builder.redis(host, port)
                .withDatabase(database)
                .build();
        RedisClient client = RedisClient.create(redisUri);
        String key = "hunyuan:it:probe:" + UUID.randomUUID();
        try (var connection = client.connect()) {
            var commands = connection.sync();
            assertThat(commands.ping()).isEqualTo("PONG");
            commands.set(key, "ok");
            assertThat(commands.get(key)).isEqualTo("ok");
            commands.del(key);
        } finally {
            client.shutdown();
        }
    }
}
