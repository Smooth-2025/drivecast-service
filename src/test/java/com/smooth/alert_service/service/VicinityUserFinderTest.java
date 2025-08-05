package com.smooth.alert_service.service;

import com.smooth.alert_service.core.VicinityUserFinder;
import com.smooth.alert_service.model.AlertEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VicinityUserFinderTest {

    @Test
    void 반경_내_사용자만_조회된다() {
        // given
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        GeoOperations<String, String> geoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);

        var redisResults = List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("alpha", new Point(0, 0)), new Distance(100)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("bravo", new Point(0, 0)), new Distance(200))
        );
        var geoResults = new GeoResults<>(redisResults);


        var event = new AlertEvent("accident", "acc-1", "user123", 37.5, 126.9, "20250804121230");
        var finder = new VicinityUserFinder(redisTemplate);

        when(geoOps.radius(
                eq("location:" + event.timestamp()),
                any(Circle.class)
        )).thenReturn(geoResults);

        // when
        var result = finder.findNearbyUsers(event);

        // then
        assertThat(result).containsExactly("alpha", "bravo");
    }

    @Test
    void 본인은_제외된다() {
        // given
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        GeoOperations<String, String> geoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(geoOps);

        var redisResults = List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("user123", new Point(0, 0)), new Distance(50)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("user999", new Point(0, 0)), new Distance(80))
        );
        var geoResults = new GeoResults<>(redisResults);


        var event = new AlertEvent("obstacle", null, "user123", 37.5, 126.9, "20250804131010");
        var finder = new VicinityUserFinder(redisTemplate);
        when(geoOps.radius(eq("location:" + event.timestamp()), any(Circle.class))).thenReturn(geoResults);

        // when
        var result = finder.findNearbyUsers(event);

        // then
        assertThat(result).containsExactly("user999");
    }

    @Test
    void 좌표가_null이면_빈_리스트_반환() {
        // given
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        var finder = new VicinityUserFinder(redisTemplate);

        var event = new AlertEvent("accident", "acc-1", "user123", null, 126.9, "20250804121230");

        // when
        var result = finder.findNearbyUsers(event);

        // then
        assertThat(result).isEmpty();
        // Redis 호출이 없어야 함
        verify(redisTemplate, never()).opsForGeo();
    }

    @Test
    void 반경이_0인_이벤트는_빈_리스트_반환() {
        // given
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        var finder = new VicinityUserFinder(redisTemplate);

        var event = new AlertEvent("start", null, "user123", 37.5, 126.9, "20250804121230");

        // when
        var result = finder.findNearbyUsers(event);

        // then
        assertThat(result).isEmpty();
        // Redis 호출이 없어야 함
        verify(redisTemplate, never()).opsForGeo();
    }}
