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

    // 기본 Redis 설정값 (내부 캐시용)
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 기본 Redis 연결 팩토리 (내부 캐시용)
     */
    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new JedisConnectionFactory(config);
    }

    /**
     * 기본 Redis용 StringRedisTemplate (내부 캐시용)
     */
    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
        org.springframework.data.redis.core.StringRedisTemplate template = 
            new org.springframework.data.redis.core.StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

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
