package com.smooth.drivecast_service.emergency.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmergencyUserInfoDto {
    private String userId;
    private String gender;
    private String bloodType;
    private String emergencyContact1;
    private String emergencyContact2;
    private String emergencyContact3;

    public boolean canNotifyFamily() {
        return (emergencyContact1 != null && !emergencyContact1.trim().isEmpty())
                || (emergencyContact2 != null && !emergencyContact2.trim().isEmpty())
                || (emergencyContact3 != null && !emergencyContact3.trim().isEmpty());
    }
}
