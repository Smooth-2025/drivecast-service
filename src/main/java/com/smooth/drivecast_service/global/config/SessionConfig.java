package com.smooth.drivecast_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import static com.smooth.drivecast_service.global.constants.GlobalConstants.WebSocket.SESSION_TIMEOUT_SEC;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = SESSION_TIMEOUT_SEC
)
public class SessionConfig {
}