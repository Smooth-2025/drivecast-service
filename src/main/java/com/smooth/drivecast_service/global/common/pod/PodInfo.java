package com.smooth.drivecast_service.global.common.pod;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * Pod 식별 정보 중앙 관리
 * Kubernetes Downward API와 로컬 환경 모두 지원
 */
@Slf4j
@Component
public class PodInfo {

    // Kubernetes Downward API 환경변수들
    @Value("${POD_NAME:#{null}}")
    private String podName;
    
    @Value("${POD_NAMESPACE:default}")
    private String podNamespace;
    
    @Value("${POD_UID:#{null}}")
    private String podUid;
    
    @Value("${POD_IP:#{null}}")
    private String podIp;
    
    // 백업용 환경변수들
    @Value("${HOSTNAME:#{null}}")
    private String hostname;
    
    private String resolvedPodId;
    private String instanceId;
    
    @PostConstruct
    public void init() {
        resolvedPodId = resolvePodId();
        instanceId = generateInstanceId();
        
        log.info("Pod 정보 초기화 완료: podId={}, instanceId={}", resolvedPodId, instanceId);
    }
    
    /**
     * 현재 Pod의 고유 식별자
     * 우선순위: POD_UID > POD_NAME > HOSTNAME > 생성된 UUID
     */
    public String getPodId() {
        return resolvedPodId;
    }
    
    /**
     * 네임스페이스 포함 전체 Pod 식별자
     */
    public String getFullPodId() {
        if (podName != null) {
            return podNamespace + "/" + podName;
        }
        return resolvedPodId;
    }
    
    /**
     * 인스턴스별 고유 ID (재시작 시마다 변경)
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * Pod IP 주소
     */
    public String getPodIp() {
        return podIp;
    }
    
    /**
     * 환경 타입 감지
     */
    public PodEnvironment getEnvironment() {
        if (podUid != null || podName != null) {
            return PodEnvironment.KUBERNETES;
        } else if (hostname != null && hostname.length() == 12) {
            return PodEnvironment.DOCKER;
        } else {
            return PodEnvironment.LOCAL;
        }
    }
    
    private String resolvePodId() {
        // 1순위: Kubernetes Pod UID (가장 안전)
        if (podUid != null && !podUid.trim().isEmpty()) {
            return podUid.trim();
        }
        
        // 2순위: Kubernetes Pod Name
        if (podName != null && !podName.trim().isEmpty()) {
            return podName.trim();
        }
        
        // 3순위: HOSTNAME (Docker/K8s 백업)
        if (hostname != null && !hostname.trim().isEmpty()) {
            return hostname.trim();
        }
        
        // 4순위: 생성된 UUID (로컬 개발)
        String generatedId = "local-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("Pod ID를 환경변수에서 찾을 수 없어 생성함: {}", generatedId);
        return generatedId;
    }
    
    private String generateInstanceId() {
        return resolvedPodId + "-" + System.currentTimeMillis();
    }
    
    public enum PodEnvironment {
        KUBERNETES, DOCKER, LOCAL
    }
}