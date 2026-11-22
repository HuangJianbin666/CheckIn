package com.example.checkin.checkin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 业务短期缓存。MySQL 是事实源，此处只做读透缓存：miss 或读取异常都回退数据库并尝试重建，
 * 写入/删除失败只记录日志，不影响已成功的数据库结果。key 内嵌业务日期，午夜后不沿用旧值。
 */
@Component
public class CheckInCache {

    private static final Logger log = LoggerFactory.getLogger(CheckInCache.class);

    private static final String TODAY_PREFIX = "checkin:biz:today:";
    private static final String STREAK_PREFIX = "checkin:biz:streak:";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CheckInCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<Set<Long>> getTodayChecked(Long userId, LocalDate date) {
        return read(todayKey(userId, date), new TypeReference<>() {
        });
    }

    public void putTodayChecked(Long userId, LocalDate date, Set<Long> habitIds) {
        write(todayKey(userId, date), habitIds);
    }

    public void deleteTodayChecked(Long userId, LocalDate date) {
        delete(todayKey(userId, date));
    }

    public Optional<StreakResult> getStreak(Long userId, Long habitId, LocalDate date) {
        return read(streakKey(userId, habitId, date), StreakResult.class);
    }

    public void putStreak(Long userId, Long habitId, LocalDate date, StreakResult streak) {
        write(streakKey(userId, habitId, date), streak);
    }

    public void deleteStreak(Long userId, Long habitId, LocalDate date) {
        delete(streakKey(userId, habitId, date));
    }

    private String todayKey(Long userId, LocalDate date) {
        return TODAY_PREFIX + userId + ":" + date;
    }

    private String streakKey(Long userId, Long habitId, LocalDate date) {
        return STREAK_PREFIX + userId + ":" + habitId + ":" + date;
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("业务缓存读取失败，回退数据库 key={}", key, e);
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, type));
        } catch (Exception e) {
            log.warn("业务缓存读取失败，回退数据库 key={}", key, e);
            return Optional.empty();
        }
    }

    private void write(String key, Object value) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), TTL);
        } catch (Exception e) {
            log.warn("业务缓存写入失败 key={}", key, e);
        }
    }

    private void delete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("业务缓存删除失败 key={}", key, e);
        }
    }
}