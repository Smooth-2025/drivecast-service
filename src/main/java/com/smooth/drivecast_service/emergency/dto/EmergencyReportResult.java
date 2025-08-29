package com.smooth.drivecast_service.emergency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmergencyReportResult {
    private final String accidentId;
    private final boolean success;
    private final String message;
    private final Boolean emergencyNotified;

    public static EmergencyReportResult reported(String accidentId, String message) {
        return new EmergencyReportResult(accidentId, true, message, true);
    }

    public static EmergencyReportResult cancelled(String accidentId, String message) {
        return new EmergencyReportResult(accidentId, true, message, false);
    }

    public static EmergencyReportResult failed(String accidentId, String message) {
        return new EmergencyReportResult(accidentId, false, message, null);
    }

}
