package com.smooth.drivecast_service.emergency.service.mapper;

import com.smooth.drivecast_service.emergency.dto.EmergencyUserInfoDto;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmergencyMapper {
    
    public EmergencyUserInfoDto toUserInfoDto(EmergencyInfoResponse.EmergencyData data) {
        if (data == null) {
            log.warn("EmergencyData가 null입니다. 기본값 반환");
            EmergencyUserInfoDto defaultDto = new EmergencyUserInfoDto();
            defaultDto.setGender("미상");
            defaultDto.setBloodType("미상");
            return defaultDto;
        }
        
        EmergencyUserInfoDto dto = new EmergencyUserInfoDto();
        dto.setUserId(data.getUserId());
        dto.setGender(data.getGender());
        dto.setBloodType(data.getBloodType());
        dto.setEmergencyContact1(data.getEmergencyContact1());
        dto.setEmergencyContact2(data.getEmergencyContact2());
        dto.setEmergencyContact3(data.getEmergencyContact3());
        
        log.debug("수동 매핑 완료: userId={}, gender={}, bloodType={}, contact1={}", 
            dto.getUserId(), dto.getGender(), dto.getBloodType(), dto.getEmergencyContact1());
        
        return dto;
    }
}