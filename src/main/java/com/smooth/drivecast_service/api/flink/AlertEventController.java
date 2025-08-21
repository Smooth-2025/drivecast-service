package com.smooth.drivecast_service.api.flink;

import com.smooth.drivecast_service.core.AlertEventHandler;
import com.smooth.drivecast_service.model.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;

/**
 * 실제 운영용 알림 이벤트 수신 API
 * - Flink에서 실시간 이벤트 수신
 * - Lambda에서 주행 시작/종료 이벤트 수신
 */
@Slf4j
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertEventController {

    private final AlertEventHandler alertEventHandler;

    /**
     * Flink에서 실시간 알림 이벤트 수신
     * - accident: 큰 사고 (반복 알림)
     * - obstacle: 장애물 (반복 알림)
     * - pothole: 포트홀 (1회성 알림)
     *
     * @param event 알림 이벤트 데이터
     * @return 처리 결과
     */
    @PostMapping("/flink")
    public ResponseEntity<Map<String, Object>> receiveFlinkEvent(@Valid @RequestBody AlertEvent event) {

        try {
            log.info("Flink 이벤트 수신: type={}, userId={}, lat={}, lng={}",
                    event.type(), event.userId(), event.latitude(), event.longitude());

            // 이벤트 처리
            alertEventHandler.handle(event);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "이벤트 처리 완료",
                    "eventType", event.type(),
                    "processedAt", Instant.now().toString()
            ));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 이벤트 데이터: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "잘못된 이벤트 데이터: " + e.getMessage(),
                    "eventType", event != null ? event.type() : "unknown"
            ));

        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "서버 내부 오류가 발생했습니다",
                    "eventType", event != null ? event.type() : "unknown"
            ));
        }
    }

    /**
     * Lambda에서 주행 시작/종료 이벤트 수신
     * - start: 주행 시작 (본인에게만 알림)
     * - end: 주행 종료 (본인에게만 알림)
     *
     * @param event 주행 이벤트 데이터
     * @return 처리 결과
     */
    @PostMapping("/lambda")
    public ResponseEntity<Map<String, Object>> receiveLambdaEvent(@Valid @RequestBody AlertEvent event) {

        try {
            log.info("Lambda 이벤트 수신: type={}, userId={}",
                    event.type(), event.userId());

            // 주행 이벤트 타입 검증
            if (!"start".equals(event.type()) && !"end".equals(event.type())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Lambda는 start/end 이벤트만 처리 가능합니다",
                        "eventType", event.type()
                ));
            }

            // 이벤트 처리
            alertEventHandler.handle(event);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "주행 이벤트 처리 완료",
                    "eventType", event.type(),
                    "processedAt", Instant.now().toString()
            ));

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 주행 이벤트 데이터: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "잘못된 이벤트 데이터: " + e.getMessage(),
                    "eventType", event != null ? event.type() : "unknown"
            ));

        } catch (Exception e) {
            log.error("주행 이벤트 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "서버 내부 오류가 발생했습니다",
                    "eventType", event != null ? event.type() : "unknown"
            ));
        }
    }

    /**
     * 알림 서비스 상태 확인
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "AlertService Event API",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "flink", "/api/alert/flink",
                        "lambda", "/api/alert/lambda"
                )
        ));
    }
}
