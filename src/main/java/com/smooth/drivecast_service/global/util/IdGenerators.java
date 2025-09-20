package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.driving.dto.DrivingEvent;
import com.smooth.drivecast_service.incident.dto.IncidentEvent;

import java.util.Optional;

public class IdGenerators {

    public static String generateIncidentAlertId(IncidentEvent event) {
        return switch (event.type()) {
            case ACCIDENT -> event.accidentId(); // 이미 검증됨
            case OBSTACLE -> String.format("obstacle-%s-%s-%s",
                    formatLatitude(event.latitude()),
                    formatLongitude(event.longitude()),
                    sanitizeTimestamp(event.timestamp()));
        };
    }

    private static String formatLatitude(Double latitude) {
        if(latitude == null) return "0";
        
        String prefix = latitude >= 0 ? "N" : "S";
        String absValue = String.valueOf(Math.abs(latitude)).replace(".", "");
        return prefix + absValue;
    }

    private static String formatLongitude(Double longitude) {
        if(longitude == null) return "0";
        
        String prefix = longitude >= 0 ? "E" : "W";
        String absValue = String.valueOf(Math.abs(longitude)).replace(".", "");
        return prefix + absValue;
    }

    private static String sanitizeTimestamp(String timestamp) {
        if(timestamp == null) return "unknown";
        return timestamp.replace(":", "").replace("-", "").replace("T", "");
    }
}