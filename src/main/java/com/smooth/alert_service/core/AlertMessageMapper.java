package com.smooth.alert_service.core;

import com.smooth.alert_service.model.AlertType;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.model.AlertMessageDto;

import java.util.Optional;

public class AlertMessageMapper {

    public static Optional<AlertMessageDto> map(AlertEvent event, String targetUserId) {
        AlertType type = AlertType.from(event.type())
                .orElseThrow(() -> new IllegalArgumentException("invalid alert type: " + event.type()));

        boolean isSelf = event.userId() != null && event.userId().equals(targetUserId);

        AlertMessageDto message = switch (type) {
            case ACCIDENT -> {
                if (isSelf) {
                    yield new AlertMessageDto(
                            "accident",
                            "큰 사고가 발생했습니다",
                            "차량에 큰 사고가 감지되었습니다. 부상이 있다면 즉시 구조를 요청하세요."
                    );
                } else {
                    yield new AlertMessageDto(
                            "accident-nearby",
                            "전방 사고 발생",
                            "전방에 큰 사고가 발생했습니다. 속도를 줄이고 주의하세요."
                    );
                }
            }
            case OBSTACLE -> {
                if (isSelf) {
                    yield null; // 본인에게는 전송하지 않음
                } else {
                    yield new AlertMessageDto(
                            "obstacle",
                            "전방 장애물 발견",
                            "전방에 장애물이 있습니다. 주의해서 운전하세요."
                    );
                }
            }
            case POTHOLE -> new AlertMessageDto(
                    "pothole",
                    "포트홀 발견",
                    "전방에 포트홀이 있습니다. 속도를 줄이고 주의해서 주행하세요."
            );
            case START -> new AlertMessageDto(
                    "start",
                    "주행 시작",
                    "안전운전하세요!"
            );
            case END -> new AlertMessageDto(
                    "end",
                    "주행 종료",
                    "수고하셨습니다. 휴식을 취하세요!"
            );
        };

        return Optional.ofNullable(message);
    }
}
