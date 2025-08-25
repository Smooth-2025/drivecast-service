package com.smooth.drivecast_service.driving.service.mapper;

import com.smooth.drivecast_service.driving.dto.DrivingMessageDto;

import java.util.Optional;

/**
 * 주행 메시지 매핑 전략 인터페이스
 * DrivingMessageDto 형식
 **/
public interface DrivingMessageMapper {

    /**
     * 이 매퍼가 처리할 수 있는 타입인지 확인
     * @param drivingType 주행 타입 (start, end)
     * @return 처리 가능하면 true
     **/
    boolean supports(String drivingType);

    /**
     * 주행 이벤트를 상태 메시지로 변환
     * @param context 매핑 컨텍스트
     * @return 변환된 AlertMessageDto, 실패시 empty
     **/
    Optional<DrivingMessageDto> map(DrivingMappingContext context);
}
