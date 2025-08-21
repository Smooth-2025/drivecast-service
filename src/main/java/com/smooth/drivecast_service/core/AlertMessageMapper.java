package com.smooth.drivecast_service.core;

import com.smooth.drivecast_service.model.AlertType;
import com.smooth.drivecast_service.model.AlertEvent;
import com.smooth.drivecast_service.model.AlertMessageDto;

import java.util.Map;
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
                            Map.of(
                                    "title", "큰 사고가 발생했습니다!",
                                    "content", "차량에 큰 사고가 감지되었습니다. 부상이 있다면 즉시 구조를 요청하세요."
                            )
                    );
                } else {
                    yield new AlertMessageDto(
                            "accident-nearby",
                            Map.of(
                                    "title", "전방 사고 발생",
                                    "content", "근처 차량에서 큰 사고가 발생했습니다. 안전 운전하세요."
                            )
                    );
                }
            }
            case OBSTACLE -> {
                if (isSelf) {
                    yield null; // 본인에게는 전송하지 않음
                } else {
                    yield new AlertMessageDto(
                            "obstacle",
                            Map.of(
                                    "title", "전방 장애물 발견",
                                    "content", "전방에 장애물이 있습니다. 주의해서 운전하세요."
                            )
                    );
                }
            }
            case POTHOLE -> new AlertMessageDto(
                    "pothole",
                    Map.of(
                            "title", "포트홀 발견",
                            "content", "전방에 포트홀이 있습니다. 속도를 줄이고 주의해서 주행하세요."
                    )
            );
            case START -> new AlertMessageDto(
                    "start",
                    Map.of(
                            "timestamp", event.timestamp()
                    )
            );
            case END -> new AlertMessageDto(
                    "end",
                    Map.of(
                            "timestamp", event.timestamp()
                    )
            );
        };

        return Optional.ofNullable(message);
    }
}
