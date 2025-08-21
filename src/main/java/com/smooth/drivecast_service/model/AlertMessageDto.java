package com.smooth.drivecast_service.model;

import java.util.Map;

public record AlertMessageDto(
        String type,
        Map<String, Object> payload
) {}
