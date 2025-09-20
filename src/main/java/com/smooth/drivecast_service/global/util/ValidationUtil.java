package com.smooth.drivecast_service.global.util;

public final class ValidationUtil {

    private ValidationUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스입니다.");
    }

    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if(latitude == null || longitude == null) return false;
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    public static boolean isValidUserId(String userId) {
        return userId == null || !userId.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean hasValidTimestampFormat(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return false;
        return timestamp.length() >= 10;
    }

    public static boolean hasValidIncidentTimestampFormat(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return false;
        if (timestamp.length() != 19) return false;
        return timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    }
}
