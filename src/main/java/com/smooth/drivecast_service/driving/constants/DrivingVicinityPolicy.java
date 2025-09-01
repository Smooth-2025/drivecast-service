package com.smooth.drivecast_service.driving.constants;

/**
 * 주행 근접 운전자 탐지 정책 상수:
 * 위치 윈도우, 세션 신선도, 재시도, 캐시 TTL, Redis 키 프리픽스
 **/
public class DrivingVicinityPolicy {

    public static final int WINDOW_SECONDS = 5; // 실시간 처리를 위한 5초 윈도우
    public static final int RADIUS_METERS = 15;

    public static final int FRESHNESS_SEC = 300; // 60초 → 300초(5분)로 대폭 완화
    public static final java.time.Duration FRESHNESS_THRESHOLD = java.time.Duration.ofSeconds(FRESHNESS_SEC);

    public static final int MAX_RETRIES = 2;
    public static final int[] RETRY_DELAYS_MS = {120, 250};

    public static final int HOT_CACHE_TTL_SEC = 20;
    public static final int WARM_CACHE_TTL_SEC = 24 * 60 * 60; // 24h

    public static final String TRAIT_HOT_PREFIX = "trait:";
    public static final String TRAIT_WARM_PREFIX = "trait:warm:";
    public static final String DRIVING_ACTIVE_SET = "driving:active";
    
    // 활성 세트 TTL (초)
    public static final int ACTIVE_SET_TTL_SEC = 60 * 60; // 1시간

    private DrivingVicinityPolicy() {}
}
