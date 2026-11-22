package com.example.checkin.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存适配器对真实 Redis（logical DB 1）的集成验证：序列化往返、TTL、按日期隔离。
 * 不启动 Spring 上下文，也不依赖 MySQL，只校验缓存适配器本身。
 */
class CheckInCacheRedisIT {

    private static final Long USER = 99001L;
    private static final Long HABIT = 99002L;

    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private CheckInCache cache;

    @BeforeEach
    void setUp() {
        factory = new LettuceConnectionFactory("127.0.0.1", 6379);
        factory.setDatabase(1);
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        cache = new CheckInCache(redis, new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }

    @AfterEach
    void tearDown() {
        Set<String> keys = redis.keys("checkin:biz:*:" + USER + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
        factory.destroy();
    }

    @Test
    void streak_roundTrips_withTtl() {
        LocalDate date = LocalDate.of(2026, 11, 21);
        StreakResult expected = new StreakResult(3, true, date);

        cache.putStreak(USER, HABIT, date, expected);

        assertThat(cache.getStreak(USER, HABIT, date)).contains(expected);
        Long ttl = redis.getExpire("checkin:biz:streak:" + USER + ":" + HABIT + ":" + date);
        assertThat(ttl).isBetween(1L, 60L);
    }

    @Test
    void todayChecked_roundTrips() {
        LocalDate date = LocalDate.of(2026, 11, 21);

        cache.putTodayChecked(USER, date, Set.of(1L, 2L, 3L));

        assertThat(cache.getTodayChecked(USER, date)).contains(Set.of(1L, 2L, 3L));
    }

    @Test
    void keys_isolateByDate() {
        LocalDate d1 = LocalDate.of(2026, 11, 20);
        LocalDate d2 = LocalDate.of(2026, 11, 21);

        cache.putStreak(USER, HABIT, d1, new StreakResult(2, false, d1.minusDays(1)));

        assertThat(cache.getStreak(USER, HABIT, d1)).isPresent();
        assertThat(cache.getStreak(USER, HABIT, d2)).isEmpty();
    }

    @Test
    void miss_returnsEmpty() {
        assertThat(cache.getStreak(USER, HABIT, LocalDate.of(2026, 11, 21))).isEmpty();
        assertThat(cache.getTodayChecked(USER, LocalDate.of(2026, 11, 21))).isEmpty();
    }
}