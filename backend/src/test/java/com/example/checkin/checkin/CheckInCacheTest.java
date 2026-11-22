package com.example.checkin.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CheckInCacheTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private CheckInCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        cache = new CheckInCache(redis, new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @Test
    void getStreak_miss_returnsEmpty() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(cache.getStreak(1L, 2L, LocalDate.of(2026, 11, 21))).isEmpty();
    }

    @Test
    void getStreak_hit_deserializes() {
        when(valueOps.get(anyString()))
                .thenReturn("{\"streak\":3,\"todayChecked\":true,\"streakAsOf\":\"2026-11-21\"}");

        Optional<StreakResult> result = cache.getStreak(1L, 2L, LocalDate.of(2026, 11, 21));

        assertThat(result).isPresent();
        assertThat(result.get().streak()).isEqualTo(3);
        assertThat(result.get().todayChecked()).isTrue();
        assertThat(result.get().streakAsOf()).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    void getStreak_readException_fallsBackEmpty() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(cache.getStreak(1L, 2L, LocalDate.of(2026, 11, 21))).isEmpty();
    }

    @Test
    void getTodayChecked_hit_deserializesSet() {
        when(valueOps.get(anyString())).thenReturn("[1,2,3]");

        Optional<Set<Long>> result = cache.getTodayChecked(1L, LocalDate.of(2026, 11, 21));

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void getTodayChecked_readException_fallsBackEmpty() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(cache.getTodayChecked(1L, LocalDate.of(2026, 11, 21))).isEmpty();
    }

    @Test
    void putStreak_setsValueAndTtlInOneCommand() {
        cache.putStreak(1L, 2L, LocalDate.of(2026, 11, 21),
                new StreakResult(3, true, LocalDate.of(2026, 11, 21)));

        verify(valueOps).set(
                eq("checkin:biz:streak:1:2:2026-11-21"),
                eq("{\"streak\":3,\"todayChecked\":true,\"streakAsOf\":\"2026-11-21\"}"),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    void put_writeException_isSwallowed() {
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        cache.putTodayChecked(1L, LocalDate.of(2026, 11, 21), Set.of(1L));
        // 不抛异常即通过
    }

    @Test
    void delete_deleteException_isSwallowed() {
        when(redis.delete(anyString())).thenThrow(new RuntimeException("redis down"));

        cache.deleteStreak(1L, 2L, LocalDate.of(2026, 11, 21));
        // 不抛异常即通过
    }

    @Test
    void keys_embedDateForIsolation() {
        when(valueOps.get(anyString())).thenReturn(null);
        LocalDate d1 = LocalDate.of(2026, 11, 20);
        LocalDate d2 = LocalDate.of(2026, 11, 21);

        cache.getTodayChecked(1L, d1);
        cache.getTodayChecked(1L, d2);

        verify(valueOps).get("checkin:biz:today:1:2026-11-20");
        verify(valueOps).get("checkin:biz:today:1:2026-11-21");
    }
}