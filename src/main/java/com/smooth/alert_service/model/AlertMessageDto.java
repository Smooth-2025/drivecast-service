package com.smooth.alert_service.model;

import java.util.Map;

public record AlertMessageDto(
        String type,
        Map<String, Object> payload
) {}
