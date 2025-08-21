package com.smooth.drivecast_service.support.util;

import com.smooth.drivecast_service.model.AlertEvent;

import java.util.Optional;
import java.util.UUID;

public class AlertIdResolver {

    public static Optional<String> resolve(AlertEvent event) {
        return switch (event.type()) {
            case "accident" -> {
                if (event.accidentId() != null) {
                    yield Optional.of(event.accidentId());
                } else {
                    // accidentId가 없으면 사용자+시간 기반으로 생성
                    String fallbackId = String.format("accident-%s-%s",
                            event.userId(),
                            event.timestamp().replace(":", "").replace("-", "").replace("T", ""));
                    yield Optional.of(fallbackId);
                }
            }
            case "obstacle" -> Optional.of("obstacle-" + UUID.randomUUID().toString());
            case "pothole" -> Optional.of("pothole-" + UUID.randomUUID().toString());
            case "start" -> Optional.of("start-" + event.userId() + "-" + event.timestamp());
            case "end" -> Optional.of("end-" + event.userId() + "-" + event.timestamp());
            default -> Optional.empty();
        };
    }
}