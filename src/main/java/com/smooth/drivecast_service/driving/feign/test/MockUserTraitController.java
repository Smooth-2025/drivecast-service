package com.smooth.drivecast_service.driving.feign.test;

import com.smooth.drivecast_service.driving.dto.TraitResponseDto;
import com.smooth.drivecast_service.driving.dto.TraitBulkResponseDto;
import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 테스트용 Mock User Trait API
 * 실제 유저 서비스 API를 시뮬레이션
 *
 * 활성화: application.yml에 mock.user-service.enabled=true 설정
 */
@Slf4j
@RestController
@RequestMapping("/internal/v1/traits")
@ConditionalOnProperty(name = "mock.user-service.enabled", havingValue = "true")
public class MockUserTraitController {

    private static final String[] CHARACTERS = {"dolphin", "lion", "eagle", "bear", "fox", "rabbit"};
    private static final Random random = new Random();

    /**
     * 단건 성향 조회 Mock API
     * GET /internal/v1/traits/{userId}
     */
    @GetMapping("/{userId}")
    public TraitResponseDto getTrait(@PathVariable String userId) {
        log.info("Mock API 호출: 단건 성향 조회 - userId={}", userId);

        // 시뮬레이션: 일부 사용자는 404 (성향 없음)
        if (userId.endsWith("99") || userId.endsWith("00")) {
            log.info("Mock API: 성향 없음 시뮬레이션 - userId={}", userId);
            throw new MockNotFoundException("성향을 찾을 수 없습니다");
        }

        // 시뮬레이션: 일부 사용자는 5xx 에러
        if (userId.endsWith("88")) {
            log.error("Mock API: 서버 오류 시뮬레이션 - userId={}", userId);
            throw new MockServerException("서버 내부 오류");
        }

        // 정상 응답: 랜덤 성향 반환
        String character = CHARACTERS[random.nextInt(CHARACTERS.length)];
        var response = new TraitResponseDto(userId, character);

        log.info("Mock API 응답: userId={}, character={}", userId, character);
        return response;
    }

    /**
     * 벌크 성향 조회 Mock API
     * GET /internal/v1/traits/bulk?hasCharacter=true
     */
    @GetMapping("/bulk")
    public TraitBulkResponseDto getTraitsBulk(@RequestParam(defaultValue = "true") boolean hasCharacter) {
        log.info("Mock API 호출: 벌크 성향 조회 - hasCharacter={}", hasCharacter);

        // 시뮬레이션: 100명의 사용자 데이터 생성
        var mockData = generateMockBulkData(100);
        var response = new TraitBulkResponseDto(mockData, Instant.now());

        log.info("Mock API 응답: 벌크 데이터 {}명 생성", mockData.size());
        return response;
    }

    /**
     * Mock 벌크 데이터 생성
     */
    private List<TraitResponseDto> generateMockBulkData(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> {
                    String userId = String.valueOf(1000 + i);
                    String character = CHARACTERS[random.nextInt(CHARACTERS.length)];
                    return new TraitResponseDto(userId, character);
                })
                .toList();
    }

    /**
     * Mock 404 예외
     */
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public static class MockNotFoundException extends RuntimeException {
        public MockNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Mock 500 예외
     */
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public static class MockServerException extends RuntimeException {
        public MockServerException(String message) {
            super(message);
        }
    }
}
