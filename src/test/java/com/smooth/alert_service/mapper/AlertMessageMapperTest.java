package com.smooth.alert_service.mapper;

import com.smooth.alert_service.core.AlertMessageMapper;
import com.smooth.alert_service.model.AlertEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertMessageMapperTest {

    @Test
    void 본인_사고는_accident_메시지로_매핑된다() {
        // 올바른 필드 순서: type, accidentId, userId, latitude, longitude, timestamp
        var event = new AlertEvent("accident", "acc-001", "driver1", 37.5, 126.9, "2025-08-04T17:00:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver1");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("accident");
        assertThat(message.title()).contains("큰 사고");
    }

    @Test
    void 다른사람의_사고는_accident_nearby_메시지로_매핑된다() {
        var event = new AlertEvent("accident", "acc-002", "driver1", 37.5, 126.9, "2025-08-04T17:00:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver2");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("accident-nearby");
        assertThat(message.title()).contains("전방 사고");
    }

    @Test
    void 본인의_장애물은_빈_Optional을_반환한다() {
        var event = new AlertEvent("obstacle", null, "driver1", 37.5, 126.9, "2025-08-04T17:01:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver1");

        assertThat(messageOpt).isEmpty();
    }

    @Test
    void 다른사람의_장애물은_obstacle_메시지로_매핑된다() {
        var event = new AlertEvent("obstacle", null, "driver1", 37.5, 126.9, "2025-08-04T17:01:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver2");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("obstacle");
        assertThat(message.title()).contains("전방 장애물");
    }

    @Test
    void 포트홀은_무조건_pothole_메시지를_반환한다() {
        var event = new AlertEvent("pothole", null, null, 37.5, 126.9, "2025-08-04T17:01:30Z");
        var messageOpt = AlertMessageMapper.map(event, "driver1");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("pothole");
        assertThat(message.title()).contains("포트홀");
    }

    @Test
    void 주행시작은_start_메시지를_반환한다() {
        var event = new AlertEvent("start", null, "driver1", null, null, "2025-08-04T17:02:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver1");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("start");
        assertThat(message.title()).contains("주행 시작");
    }

    @Test
    void 주행종료는_end_메시지를_반환한다() {
        var event = new AlertEvent("end", null, "driver1", null, null, "2025-08-04T17:03:00Z");
        var messageOpt = AlertMessageMapper.map(event, "driver1");

        assertThat(messageOpt).isPresent();
        var message = messageOpt.get();
        assertThat(message.type()).isEqualTo("end");
        assertThat(message.title()).contains("주행 종료");
    }

    @Test
    void 잘못된_type이면_예외가_발생한다() {
        var event = new AlertEvent("invalid", null, "driver1", null, null, "2025-08-04T17:04:00Z");

        assertThatThrownBy(() -> AlertMessageMapper.map(event, "driver1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid alert type");
    }
}