package com.smooth.drivecast_service.global.common.notification;

public interface RealtimePublisher {

    void toUser(String userId, String destination, Object payload);
}
