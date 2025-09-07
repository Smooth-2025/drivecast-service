package com.smooth.drivecast_service.global.config;

import com.smooth.drivecast_service.global.security.JwtTokenProvider;
import com.smooth.drivecast_service.global.security.StompPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public ThreadPoolTaskScheduler brokerTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(2);
        ts.setThreadNamePrefix("ws-heartbeat-");
        ts.initialize();
        return ts;
    }

    @Value("${ws.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic")
                .setTaskScheduler(brokerTaskScheduler())
                .setHeartbeatValue(new long[]{30000, 30000});
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && accessor.getCommand() != null) {
                    switch (accessor.getCommand()) {
                        case CONNECT:
                            // JWT 검증 강화: 실패 시 연결 거부
                            String token = accessor.getFirstNativeHeader("Authorization");
                            if (token == null) {
                                token = accessor.getFirstNativeHeader("authorization");
                            }
                            
                            if (token == null || !token.startsWith("Bearer ")) {
                                log.warn("WebSocket CONNECT 거부: Authorization 헤더 없음");
                                throw new IllegalArgumentException("Authorization header required");
                            }
                            
                            try {
                                String userId = jwtTokenProvider.getUserId(token.substring(7));
                                accessor.setUser(new StompPrincipal(userId));
                                log.debug("WebSocket CONNECT 성공: userId={}", userId);
                            } catch (Exception e) {
                                log.warn("WebSocket CONNECT 거부: JWT 검증 실패 - {}", e.getMessage());
                                throw new IllegalArgumentException("Invalid JWT token");
                            }
                            break;
                        case DISCONNECT:
                            log.debug("WebSocket DISCONNECT - sessionId = {}", accessor.getSessionId());
                            break;
                    }
                }
                return message;
            }
        });
    }
}
