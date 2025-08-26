package com.smooth.drivecast_service.driving.service.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrivingMessageMapperFactory {

    private final List<DrivingMessageMapper> mappers;

    /**
     * 주행 타입에 맞는 매퍼 조회
     * @param drivingType 주행 타입 (start, end)
     * @return 해당 타입을 처리할 수 있는 매퍼
     **/
    public Optional<DrivingMessageMapper> get(String drivingType) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(drivingType))
                .findFirst()
                .or(()->{
                    log.warn("지원하지 않는 주행 타입: {}", drivingType);
                    return Optional.empty();
                });
    }

    /**
     * 등록된 매퍼 목록 조회 (디버깅용)
     **/
    public List<String> getSupportedTypes() {
        return mappers.stream()
                .map(mapper -> mapper.getClass().getSimpleName())
                .toList();
    }
}
