package com.smooth.drivecast_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // Valkey 설정값
    @Value("${valkey.host:localhost}")
    private String valkeyHost;

    @Value("${valkey.port:6379}")
    private int valkeyPort;

    @Value("${valkey.database:1}")
    private int valkeyDatabase;

    @Value("${valkey.password:}")
    private String valkeyPassword;

    /**
     * 기본 Redis는 Spring Boot 자동 설정 사용 (캐시용)
     * 기본 RedisTemplate은 Spring Boot가 자동 생성 (캐시용으로 사용) */

    // Valkey용 별도 설정
    @Bean
    public JedisConnectionFactory valkeyConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(valkeyHost);
        config.setPort(valkeyPort);
        config.setDatabase(valkeyDatabase);
        if (!valkeyPassword.isEmpty()) {
            config.setPassword(valkeyPassword);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> valkeyRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(valkeyConnectionFactory());

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
