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

    /**
     * 사고 타입에 맞는 매퍼 조회
     * @param incidentType 사고 타입 (accident, obstacle)
     * @return 해당 타입을 처리할 수 있는 매퍼
     **/
    public Optional<IncidentMessageMapper> get(String incidentType) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(incidentType))
                .findFirst()
                .or(()->{
                    log.warn("지원하지 않는 사고 타입: {}", incidentType);
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
