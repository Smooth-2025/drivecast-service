package com.smooth.alert_service.core;

import com.smooth.alert_service.model.EventType;
import com.smooth.alert_service.model.AlertEvent;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VicinityUserFinder {

    private final RedisTemplate<String, String> redisTemplate;

    public VicinityUserFinder(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<String> findNearbyUsers(AlertEvent event) {
        var typeOpt = EventType.from(event.type());
        if (typeOpt.isEmpty()) {
            throw new IllegalArgumentException("Unsupported event type: " + event.type());
        }

        int radius = typeOpt.get().getRadiusMeters();
        if (radius == 0) return List.of();

        if (event.latitude() == null || event.longitude() == null) {
            return List.of();
        }

        String key = "location:" + event.timestamp();

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                geoOps.radius(key,
                        new Circle(
                                new Point(event.longitude(), event.latitude()),
                                new Distance(radius)
                        ));

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(GeoResult::getContent)
                .map(RedisGeoCommands.GeoLocation::getName)
                .filter(name -> !name.equals(event.userId()))
                .toList();
    }
}
