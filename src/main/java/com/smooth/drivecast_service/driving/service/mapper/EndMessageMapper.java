package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.driving.dto.DrivingResponseDto;
import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class EndMessageMapper implements  DrivingMessageMapper{

    @Override
    public boolean supports(String drivingType) {
        return "end".equalsIgnoreCase(drivingType);
    }

    @Override
    public Optional<DrivingResponseDto> map(DrivingMappingContext context) {
        try {
            var event = context.getEvent();

            return Optional.of(new DrivingResponseDto(
                    "end",
                    Map.of(
                            "timestamp", event.timestamp()
                    )
            ));
        } catch (Exception e) {
            log.error("END 메시지 매핑 실패: context={}", context, e);
            throw new BusinessException(DrivingErrorCode.DRIVING_MESSAGE_MAPPING_FAILED, e.getMessage());
        }
    }
}
