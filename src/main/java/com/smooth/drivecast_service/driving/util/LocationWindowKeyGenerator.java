package com.smooth.drivecast_service.driving.util;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;
import com.smooth.drivecast_service.global.util.KoreanTimeUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 위치 윈도우 키 생성 유틸리티:
 * 최근 N초 윈도우에 해당하는 location 키들을 생성
 */
public class LocationWindowKeyGenerator {

    /**
     * 기준 시각으로부터 윈도우 범위의 위치 키들을 생성
     * @param refUtc 기준 시각 (UTC)
     * @param windowSec 윈도우 크기 (초)
     * @return 위치 키 리스트 (최신순)
     **/
    public static List<String> generateWindowKeys(Instant refUtc, int windowSec) {
        List<String> keys = new ArrayList<>();

        // 미래 1초 포함 (람다 지연 대응)
        Instant futureTime = refUtc.plusSeconds(1);
        String futureKey = KoreanTimeUtil.toLocationKey(futureTime);
        keys.add(futureKey);

        // 현재부터 과거까지
        for(int i = 0; i < windowSec; i++) {
            Instant targetTime = refUtc.minusSeconds(i);
            String locationKey = KoreanTimeUtil.toLocationKey(targetTime);
            keys.add(locationKey);
        }

        return keys;
    }

    /**
     * 현재 시각 기준으로 기본 윈도우 키들을 생성
     * @param windowSec 윈도우 크기 (초)
     * @return 위치 키 리스트
     **/
    public static List<String> generateCurrentWindowKeys(int windowSec) {
        return generateWindowKeys(Instant.now(), DrivingVicinityPolicy.WINDOW_SECONDS);
    }

    /**
     * 특정 시각 기준으로 기본 윈도우 키들을 생성
     * @param refUtc 기준 시각 (UTC)
     * @return 위치 키 리스트
     **/
    public static List<String> generateDefaultWindowKeys(Instant refUtc) {
      return generateWindowKeys(refUtc, DrivingVicinityPolicy.WINDOW_SECONDS);
    }

}
