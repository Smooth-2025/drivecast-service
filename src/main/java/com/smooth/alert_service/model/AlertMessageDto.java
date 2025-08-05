package com.smooth.alert_service.model;

public record AlertMessageDto(
        String type,
        String title,
        String content
) {}
