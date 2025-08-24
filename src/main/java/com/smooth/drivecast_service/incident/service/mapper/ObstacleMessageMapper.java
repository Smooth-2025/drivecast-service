package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.model.AlertMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ObstacleMessageMapper implements IncidentMessageMapper {

    @Override
    public boolean supports(String incidentType) {
        return "obstacle".equalsIgnoreCase(incidentType);
    }

    @Override
    public Optional<AlertMessageDto> map(IncidentMappingContext context) {
        try {
            var event = context.getEvent();

            return Optional.of(new AlertMessageDto(
                    "obstacle",
                    Map.of(
                            "title", "전방 장애물 발견",
                            "content", "전방에 장애물이 있습니다. 주의해서 운전하세요."
                    )
            ));
        } catch (Exception e) {
            log.error("OBSTACLE 메시지 매핑 실패: context={}", context, e);
            return Optional.empty();
        }
    }
}
