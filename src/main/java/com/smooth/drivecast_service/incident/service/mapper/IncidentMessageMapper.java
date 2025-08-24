package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.incident.dto.IncidentMessageDto;

import java.util.Optional;

/**
 * 사고 메시지 매핑 전략 인터페이스
 * 기존 AlertMessageDto 형식 유지
 **/
public interface IncidentMessageMapper {

    /**
     * 이 매퍼가 처리할 수 있는 타입인지 확인
     * @param incidentType 사고 타입 (accident, obstacle)
     * @return 처리 가능하면 true
     **/
    boolean supports(String incidentType);

    /**
     * 사고 이벤트를 알림 메시지로 변환
     * @param context 매핑 컨텍스트
     * @return 변환된 AlertMessageDto, 실패시 empty
     **/
    Optional<IncidentMessageDto> map(IncidentMappingContext context);
}
