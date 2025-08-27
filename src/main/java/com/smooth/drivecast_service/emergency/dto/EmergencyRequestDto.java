package com.smooth.drivecast_service.emergency.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmergencyRequestDto {
    @NonNull
    private String accidentId;
    @NonNull
    private Long userId;
    private Boolean chooseReport;
    private boolean timeout;
}
