package com.example.checkin.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.checkin.habit.HabitListResponse;
import com.example.checkin.habit.HabitService;
import com.example.checkin.user.User;
import com.example.checkin.user.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * 通过服务层验证业务缓存的读写与失效：使用真实 MySQL（checkin_test）与真实 Redis（DB 1）。
 */
@SpringBootTest
@ActiveProfiles("test")
class CheckInServiceCacheIT {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HabitService habitService;
    @Autowired private CheckInService checkInService;
    @Autowired private CheckInCache cache;
    @Autowired private StringRedisTemplate redis;
    @Autowired private Clock clock;
    @Autowired private JdbcTemplate jdbc;

    private Long userId;
    private Long habitId;

    @BeforeEach
    void setUp() {
        String username = "it_cache_" + System.nanoTime();
        userId = userRepository.save(new User(username, passwordEncoder.encode("password123"))).getId();
        habitId = habitService.create(userId, "阅读").id();
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            jdbc.update("DELETE FROM check_in_records WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM habits WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM users WHERE id = ?", userId);
        }
        if (userId != null) {
            Set<String> keys = redis.keys("checkin:biz:*:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        }
    }

    @Test
    void checkIn_invalidatesTodayAndStreakCache() {
        LocalDate today = LocalDate.now(clock);
        String todayKey = "checkin:biz:today:" + userId + ":" + today;
        String streakKey = "checkin:biz:streak:" + userId + ":" + habitId + ":" + today;

        HabitListResponse first = habitService.list(userId);
        assertThat(first.items()).singleElement().satisfies(item -> {
            assertThat(item.todayChecked()).isFalse();
            assertThat(item.streak()).isZero();
        });
        assertThat(redis.hasKey(todayKey)).isTrue();
        assertThat(redis.hasKey(streakKey)).isTrue();

        checkInService.checkIn(userId, habitId);

        assertThat(redis.hasKey(todayKey)).isFalse();
        assertThat(redis.hasKey(streakKey)).isFalse();
    }

    @Test
    void list_servesCachedStreakWithoutHittingDb() {
        LocalDate today = LocalDate.now(clock);

        // 第一次 list 重建缓存（streak=0）
        habitService.list(userId);

        // 篡改缓存为错误值，证明第二次 list 直接读缓存而非数据库
        cache.putStreak(userId, habitId, today, new StreakResult(99, false, today.minusDays(1)));

        HabitListResponse second = habitService.list(userId);
        assertThat(second.items()).singleElement()
                .satisfies(item -> assertThat(item.streak()).isEqualTo(99));
    }
}