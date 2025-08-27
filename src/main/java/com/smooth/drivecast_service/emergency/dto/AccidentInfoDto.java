package com.smooth.drivecast_service.emergency.dto;

import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccidentInfoDto {
    private String accidentId;
    private String userId;
    private Double latitude;
    private Double longitude;
    private String accidentTime;

    public static AccidentInfoDto fromAlertEvent(IncidentEvent event) {
        return new AccidentInfoDto(
                event.accidentId(),
                event.userId(),
                event.latitude(),
                event.longitude(),
                event.timestamp()
        );
    }
}
