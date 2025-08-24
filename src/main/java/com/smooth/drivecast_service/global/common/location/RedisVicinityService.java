package com.smooth.drivecast_service.global.common.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisVicinityService implements VicinityService{

    private final VicinityUserFinder vicinityUserFinder;

    @Override
    public List<String> findUsers(double latitude,
                                  double longitude,
                                  int radiusMeters,
                                  boolean includeSelf,
                                  int freshnessSec,
                                  int maxRetries,
                                  List<Long> retryDelaysMs,
                                  Instant refTime,
                                  String excludeUserId) {

        // includeSelf가 false면 excludeUserId 설정
        String actualExcludeUserId = includeSelf ? null : excludeUserId;

        // 기존 메서드 호출 (재시도 로직은 기존 구현 활용)
        return vicinityUserFinder.findUsersAround(
                latitude,
                longitude,
                radiusMeters,
                refTime,
                Duration.ofSeconds(freshnessSec),
                actualExcludeUserId
        );
    }
}
