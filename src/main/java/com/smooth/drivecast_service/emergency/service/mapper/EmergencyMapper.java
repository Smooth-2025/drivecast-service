package com.smooth.drivecast_service.emergency.service.mapper;

import com.smooth.drivecast_service.emergency.dto.EmergencyUserInfoDto;
import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmergencyMapper {
    
    EmergencyUserInfoDto toUserInfoDto(EmergencyInfoResponse.EmergencyData data);
}