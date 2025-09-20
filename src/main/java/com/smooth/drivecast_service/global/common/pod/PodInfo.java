package com.smooth.drivecast_service.global.common.pod;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Slf4j
@Component
public class PodInfo {

    @Value("${POD_NAME:#{null}}")
    private String podName;
    
    @Value("${POD_NAMESPACE:default}")
    private String podNamespace;
    
    @Value("${POD_UID:#{null}}")
    private String podUid;
    
    @Value("${POD_IP:#{null}}")
    private String podIp;

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

    public String getPodId() {
        return resolvedPodId;
    }
    
    private String resolvePodId() {
        if (podUid != null && !podUid.trim().isEmpty()) {
            return podUid.trim();
        }

        if (podName != null && !podName.trim().isEmpty()) {
            return podName.trim();
        }

        if (hostname != null && !hostname.trim().isEmpty()) {
            return hostname.trim();
        }

        String generatedId = "local-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("Pod ID를 환경변수에서 찾을 수 없어 생성함: {}", generatedId);
        return generatedId;
    }
    
    private String generateInstanceId() {
        return resolvedPodId + "-" + System.currentTimeMillis();
    }
}