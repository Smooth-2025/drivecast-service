package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.dto.DrivingDestinations;
import com.smooth.drivecast_service.driving.dto.DrivingEvent;
import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.driving.service.mapper.DrivingMappingContext;
import com.smooth.drivecast_service.driving.service.mapper.DrivingMessageMapperFactory;
import com.smooth.drivecast_service.global.common.notification.RealtimePublisher;
import com.smooth.drivecast_service.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingEventHandler {

    private final DrivingMessageMapperFactory drivingMessageMapperFactory;
    private final RealtimePublisher publisher;

    public void handle(DrivingEvent event) {
        log.info("주행 이벤트 처리 시작: type={}, userId={}, timestamp={}",
                event.type(), event.userId(), event.timestamp());

        try {
            sendToSelf(event);

            log.info("주행 이벤트 처리 완료: type={}, userId={}",
                    event.type(), event.userId());
        } catch (Exception e) {
            log.error("주행 이벤트 처리 중 오류 발생: type={}, userId={}",
                    event.type(), event.userId(), e);
            throw new BusinessException(DrivingErrorCode.DRIVING_EVENT_PROCESSING_FAILED, e.getMessage());
        }
    }

    private void sendToSelf(DrivingEvent event) {
        if (event.userId() == null || event.userId().isBlank()) {
            log.warn("주행 이벤트에 userId 없음: {}", event);
            throw new BusinessException(DrivingErrorCode.MISSING_USER_ID);
        }

        try {
            var context = DrivingMappingContext.of(event);
            var mapper = drivingMessageMapperFactory.get(event.type().getValue());

            if (mapper.isEmpty()) {
                log.warn("지원하지 않는 주행 타입: {}", event.type());
                throw new BusinessException(DrivingErrorCode.INVALID_DRIVING_TYPE, "지원하지 않는 주행 타입: " + event.type());
            }

            mapper.get().map(context).ifPresentOrElse(
                    message -> {
                        publisher.toUser(event.userId(), DrivingDestinations.DRIVING_STATUS, message);
                        log.info("주행 알림 전송 완료: type={}, userId={}", event.type(), event.userId());
                    },
                    () -> {
                        log.warn("주행 메시지 매핑 결과 없음: type={}", event.type());
                        throw new BusinessException(DrivingErrorCode.DRIVING_MESSAGE_MAPPING_FAILED);
                    }
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("주행 알림 전송 중 오류 발생: userId={}", event.userId(), e);
            throw new BusinessException(DrivingErrorCode.DRIVING_NOTIFICATION_FAILED, e.getMessage());
        }
    }
}
