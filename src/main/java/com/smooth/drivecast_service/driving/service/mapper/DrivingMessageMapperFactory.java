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

    public Optional<DrivingMessageMapper> get(String drivingType) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(drivingType))
                .findFirst()
                .or(()->{
                    log.warn("지원하지 않는 주행 타입: {}", drivingType);
                    return Optional.empty();
                });
    }
}
