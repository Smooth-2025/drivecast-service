package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.global.util.KoreanTimeUtil;

import java.time.Instant;

/**
 * 위치 키 생성 유틸리티
 * 정책 없는 순수 키 생성 로직만 포함
 **/
public class LocationKeyGenerator {

    /**
     * 특정 시각의 위치 키 생성
     * @param instant 시각
     * @return 위치 키 (예: location:20250822143000)
     **/
    public static String generate(Instant instant) {
        return KoreanTimeUtil.toLocationKey(instant);
    }

    /**
     * 현재 시각의 위치 키 생성
     * @return 현재 위치 키
     **/
    public static String getCurrentKey() {
        return KoreanTimeUtil.getCurrentLocationKey();
    }

    /**
     * 한국시 문자열로부터 위치 키 생성
     * @param koreanTimeString 한국시 문자열 (예: "2025-08-22T14:30:00")
     * @return 위치 키
     **/
    public static String fromKoreanTime(String koreanTimeString) {
        return KoreanTimeUtil.koreanTimeToLocationKey(koreanTimeString);
    }
}
