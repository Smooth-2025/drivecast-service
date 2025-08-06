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

    public List<String> findUsersAround(double lat, double lon, String key, double radiusMeters, String excludeUserId) {
        if (radiusMeters == 0) return List.of();

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                geoOps.radius(key,
                        new Circle(
                                new Point(lon, lat),
                                new Distance(radiusMeters)
                        ));

        if (results == null) return List.of();

        return results.getContent().stream()
                .map(GeoResult::getContent)
                .map(RedisGeoCommands.GeoLocation::getName)
                .filter(name -> !name.equals(excludeUserId))
                .toList();
    }
}
