package com.smooth.drivecast_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * IncidentRepeatNotifier의 반복 알림을 위한 비동기 지원
 **/
@Configuration
@EnableAsync
public class AsyncConfig {
}