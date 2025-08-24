package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.model.AlertMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class StartMessageMapper implements  DrivingMessageMapper{

    @Override
    public boolean supports(String drivingType) {
        return "start".equalsIgnoreCase(drivingType);
    }

    @Override
    public Optional<AlertMessageDto> map(DrivingMappingContext context) {
        try {
            var event = context.getEvent();

            return Optional.of(new AlertMessageDto(
                    "start",
                    Map.of(
                            "timestamp", event.timestamp()
                    )
            ));
        } catch (Exception e) {
            log.error("START 메시지 매핑 실패: context={}", context, e);
            return Optional.empty();
        }
    }
}
