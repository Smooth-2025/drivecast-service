package com.smooth.drivecast_service.incident.service.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentMessageMapperFactory {

    private final List<IncidentMessageMapper> mappers;

    public Optional<IncidentMessageMapper> get(String incidentType) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(incidentType))
                .findFirst()
                .or(()->{
                    log.warn("지원하지 않는 사고 타입: {}", incidentType);
                    return Optional.empty();
                });
    }
}
