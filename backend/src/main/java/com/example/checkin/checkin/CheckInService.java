package com.example.checkin.checkin;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 打卡的非事务协调器：等待 {@link CheckInWriter} 提交后再计算连续天数并返回。
 * 后续缓存失效发生在此之后，缓存失败不回滚已成功的数据库写入。
 */
@Service
public class CheckInService {

    private final CheckInWriter writer;
    private final CheckInRecordJdbcRepository repository;
    private final StreakService streakService;
    private final CheckInCache cache;
    private final Clock clock;

    public CheckInService(CheckInWriter writer, CheckInRecordJdbcRepository repository,
                          StreakService streakService, CheckInCache cache, Clock clock) {
        this.writer = writer;
        this.repository = repository;
        this.streakService = streakService;
        this.cache = cache;
        this.clock = clock;
    }

    public CheckInResponse checkIn(Long userId, Long habitId) {
        CheckInRecordRow record = writer.write(userId, habitId);
        LocalDate today = LocalDate.now(clock);
        Set<LocalDate> dates = repository.findDates(userId, habitId, today);
        StreakResult streak = streakService.calculate(dates, today);
        // writer 已提交，此处失效缓存；缓存失败不回滚已成功的数据库打卡。
        cache.deleteTodayChecked(userId, today);
        cache.deleteStreak(userId, habitId, today);
        return CheckInResponse.of(record, streak);
    }
}