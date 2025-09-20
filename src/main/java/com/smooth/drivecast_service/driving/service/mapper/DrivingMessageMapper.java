package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.driving.dto.DrivingResponseDto;

import java.util.Optional;

public interface DrivingMessageMapper {

    boolean supports(String drivingType);

    Optional<DrivingResponseDto> map(DrivingMappingContext context);
}
