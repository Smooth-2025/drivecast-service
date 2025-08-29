package com.smooth.drivecast_service.driving.feign.test;

import com.smooth.drivecast_service.driving.service.DrivingTraitService;
import com.smooth.drivecast_service.driving.service.TraitCacheService;
import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 성향 서비스 테스트용 컨트롤러
 * Mock API와 실제 서비스 로직을 테스트
 */
@Slf4j
@RestController
@RequestMapping("/api/test/traits")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mock.user-service.enabled", havingValue = "true")
public class MockTraitTestController {

    private final DrivingTraitService drivingTraitService;
    private final TraitCacheService traitCacheService;

    /**
     * 단건 성향 조회 테스트
     */
    @GetMapping("/single/{userId}")
    public ApiResponse<Map<String, Object>> testSingleTrait(@PathVariable String userId) {
        log.info("단건 성향 조회 테스트: userId={}", userId);

        var character = drivingTraitService.getTrait(userId);

        // HashMap으로 명시적 타입 지정
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("character", character != null ? character : "없음");
        result.put("success", character != null);

        return ApiResponse.success("단건 성향 조회 테스트 완료", result);
    }

    /**
     * 벌크 성향 조회 테스트
     */
    @GetMapping("/bulk")
    public ApiResponse<Map<String, Object>> testBulkTraits() {
        log.info("벌크 성향 조회 테스트 시작");

        var traits = drivingTraitService.exportTraits();

        // 샘플 데이터 추출
        var sampleData = traits.entrySet().stream()
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        // HashMap으로 명시적 타입 지정
        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", traits.size());
        result.put("sampleData", sampleData);
        result.put("success", !traits.isEmpty());

        return ApiResponse.success("벌크 성향 조회 테스트 완료", result);
    }

    /**
     * 다중 사용자 성향 조회 테스트 (캐시 + API)
     */
    @PostMapping("/multiple")
    public ApiResponse<Map<String, Object>> testMultipleTraits(@RequestBody List<String> userIds) {
        log.info("다중 성향 조회 테스트: userIds={}", userIds);

        var traits = drivingTraitService.getTraitsFromApi(userIds);

        // HashMap으로 명시적 타입 지정
        Map<String, Object> result = new HashMap<>();
        result.put("requestedUsers", userIds.size());
        result.put("foundTraits", traits.size());
        result.put("traits", traits);
        result.put("success", true);

        return ApiResponse.success("다중 성향 조회 테스트 완료", result);
    }

    /**
     * 캐시 상태 확인
     */
    @GetMapping("/cache/stats")
    public ApiResponse<TraitCacheService.CacheStats> getCacheStats() {
        log.info("캐시 상태 조회 테스트");

        var stats = traitCacheService.getCacheStats();
        return ApiResponse.success("캐시 상태 조회 완료", stats);
    }

    /**
     * 수동 워밍 캐시 실행
     */
    @PostMapping("/cache/warmup")
    public ApiResponse<String> manualWarmup() {
        log.info("수동 워밍 캐시 테스트 시작");

        traitCacheService.manualWarmup();

        return ApiResponse.success("수동 워밍 캐시 실행 완료", "워밍 캐시가 실행되었습니다");
    }
}
