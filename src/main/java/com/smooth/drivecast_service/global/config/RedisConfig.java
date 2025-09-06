package com.smooth.drivecast_service.global.config;

import com.smooth.drivecast_service.global.common.messaging.PubSubMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;
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

    // 메시징용 Redis 설정값 (배포/개발 환경 모두 지원)
    @Value("${messaging.redis.host:${messaging-redis.host:localhost}}")
    private String messagingRedisHost;

    @Value("${messaging.redis.port:${messaging-redis.port:6379}}")
    private int messagingRedisPort;

    @Value("${messaging.redis.database:${messaging-redis.database:2}}")
    private int messagingRedisDatabase;

    @Value("${messaging.redis.password:${messaging-redis.password:}}")
    private String messagingRedisPassword;

    /**
     * 기본 Redis 연결 팩토리 (내부 캐시용)
     */
    @Bean("redisConnectionFactory")
    @Primary
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
     * 기본 Redis용 StringRedisTemplate (내부 캐시용 - lastseen 등)
     */
    @Bean("stringRedisTemplate")
    @Primary
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
        org.springframework.data.redis.core.StringRedisTemplate template =
            new org.springframework.data.redis.core.StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    /**
     * 기본 RedisTemplate (Spring Boot 자동 설정 대체)
     */
    @Bean
    public RedisTemplate<Object, Object> redisTemplate() {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
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

    // 메시징용 Redis 설정
    @Bean
    public JedisConnectionFactory messagingRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(messagingRedisHost);
        config.setPort(messagingRedisPort);
        config.setDatabase(messagingRedisDatabase);
        if (!messagingRedisPassword.isEmpty()) {
            config.setPassword(messagingRedisPassword);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean("messagingStringRedisTemplate")
    public StringRedisTemplate messagingStringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(messagingRedisConnectionFactory());
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "messaging.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            PubSubMessageListener messageListener,
            com.smooth.drivecast_service.global.common.messaging.KickMessageListener kickMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(messagingRedisConnectionFactory());
        
        // WebSocket 메시지 채널
        container.addMessageListener(messageListener, new ChannelTopic("websocket:messages"));
        
        // 킥 시스템 채널
        container.addMessageListener(kickMessageListener, new ChannelTopic("ws:system:kick"));
        
        return container;
    }
}
