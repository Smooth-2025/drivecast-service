package com.smooth.alert_service.dto;

public record AlertMessageDto(
        String type,
        String title,
        String content
) {}
