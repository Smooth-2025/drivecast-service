package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.incident.dto.IncidentMessageDto;
import com.smooth.drivecast_service.incident.exception.IncidentErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AccidentMessageMapper implements  IncidentMessageMapper{

    @Override
    public boolean supports(String incidentType) {
        return "accident".equalsIgnoreCase(incidentType);
    }

    @Override
    public Optional<IncidentMessageDto> map(IncidentMappingContext context) {
        try {
            if (context.isSelfIncident()) {
                return Optional.of(new IncidentMessageDto(
                        "accident",
                        Map.of(
                                "title", "큰 사고가 발생했습니다!",
                                "content", "차량에 큰 사고가 감지되었습니다. 부상이 있다면 즉시 구조를 요청하세요."
                        )
                ));
            } else {
                return Optional.of(new IncidentMessageDto(
                        "accident-nearby",
                        Map.of(
                                "title", "전방 사고 발생!",
                                "content", "근처 차량에서 큰 사고가 발생했습니다. 안전 운전하세요."
                        )
                ));
            }
        } catch (Exception e) {
            log.error("ACCIDENT 메시지 매핑 실패: context={}", context, e);
            throw new BusinessException(IncidentErrorCode.INCIDENT_MESSAGE_MAPPING_FAILED, e.getMessage());
        }
    }
}
