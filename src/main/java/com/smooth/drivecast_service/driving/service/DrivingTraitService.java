package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.driving.feign.UserTraitClient;
import com.smooth.drivecast_service.global.exception.BusinessException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 성향 API 서비스
 * 외부 API 호출 및 예외 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingTraitService {

    private final UserTraitClient userTraitClient;

    // 유효한 성향 enum 값들 (검증용)
    private static final Set<String> VALID_CHARACTERS = Set.of(
            "dolphin", "lion", "meerkat", "cat"
    );

    /**
     * 벌크 성향 조회 (워밍 캐시용)
     */
    public Map<String, String> exportTraits() {
        try {
            var response = userTraitClient.getTraitsBulk(true);

            if (response == null || !response.hasData()) {
                log.warn("벌크 성향 조회 결과 없음");
                return Map.of();
            }

            var result = new HashMap<String, String>();
            response.data().forEach(trait -> {
                if (trait.hasCharacter() && isValidCharacter(trait.character())) {
                    result.put(trait.userId(), trait.character());
                } else if (trait.hasCharacter()) {
                    log.warn("유효하지 않은 성향 값: userId={}, character={}",
                            trait.userId(), trait.character());
                }
            });

            log.info("벌크 성향 조회 완료: 전체={}명, 유효={}명, 생성시각={}",
                    response.data().size(), result.size(), response.generatedAtUtc());
            return result;

        } catch (FeignException.NotFound e) {
            log.warn("벌크 성향 API 404: {}", e.getMessage());
            return Map.of();

        } catch (FeignException e) {
            if (e.status() >= 500) {
                log.error("벌크 성향 API 서버 오류: status={}, 메시지={}", e.status(), e.getMessage());
                throw new BusinessException(DrivingErrorCode.TRAIT_BULK_EXPORT_FAILED,
                        "벌크 성향 조회 서버 오류: " + e.status());
            } else {
                log.warn("벌크 성향 API 클라이언트 오류: status={}, 메시지={}", e.status(), e.getMessage());
                return Map.of();
            }

        } catch (Exception e) {
            log.error("벌크 성향 조회 예외", e);
            throw new BusinessException(DrivingErrorCode.TRAIT_BULK_EXPORT_FAILED, e.getMessage());
        }
    }

    /**
     * 단건 성향 조회 (실시간 폴백용)
     */
    public String getTrait(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            var response = userTraitClient.getTrait(userId);

            if (response != null && response.hasCharacter()) {
                if (isValidCharacter(response.character())) {
                    log.debug("단건 성향 조회 성공: userId={}, character={}", userId, response.character());
                    return response.character();
                } else {
                    log.warn("유효하지 않은 성향 값: userId={}, character={}", userId, response.character());
                    return null;
                }
            }

            log.debug("성향 없음: userId={}", userId);
            return null;

        } catch (FeignException.NotFound e) {
            log.debug("성향 없음 (404): userId={}", userId);
            return null;

        } catch (FeignException e) {
            if (e.status() >= 500) {
                log.warn("단건 성향 API 서버 오류: userId={}, status={}", userId, e.status());
            } else {
                log.debug("단건 성향 API 클라이언트 오류: userId={}, status={}", userId, e.status());
            }
            return null;

        } catch (Exception e) {
            log.warn("단건 성향 조회 예외: userId={}, 오류={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 여러 사용자 성향 조회 (순수 API 호출만)
     */
    public Map<String, String> getTraitsFromApi(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<String, String>();

        for (String userId : userIds) {
            var character = getTrait(userId);
            if (character != null) {
                result.put(userId, character);
            }
        }

        log.debug("API 성향 조회 완료: 요청={}명, 조회={}명", userIds.size(), result.size());
        return result;
    }

    /**
     * 성향 값 유효성 검증
     */
    private boolean isValidCharacter(String character) {
        return character != null && VALID_CHARACTERS.contains(character.toLowerCase());
    }
}
