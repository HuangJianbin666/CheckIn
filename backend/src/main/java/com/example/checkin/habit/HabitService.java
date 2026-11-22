package com.example.checkin.habit;

import com.example.checkin.checkin.CheckInCache;
import com.example.checkin.checkin.CheckInRecordJdbcRepository;
import com.example.checkin.checkin.StreakResult;
import com.example.checkin.checkin.StreakService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final CheckInRecordJdbcRepository recordRepository;
    private final StreakService streakService;
    private final CheckInCache cache;
    private final Clock clock;

    public HabitService(HabitRepository habitRepository,
                        CheckInRecordJdbcRepository recordRepository,
                        StreakService streakService,
                        CheckInCache cache,
                        Clock clock) {
        this.habitRepository = habitRepository;
        this.recordRepository = recordRepository;
        this.streakService = streakService;
        this.cache = cache;
        this.clock = clock;
    }

    // 新增打卡项无当日记录，只需失效当日状态 key；Redis 删除在 save 提交后执行。
    public HabitResponse create(Long userId, String name) {
        Habit habit = habitRepository.save(new Habit(userId, name.trim()));
        cache.deleteTodayChecked(userId, LocalDate.now(clock));
        return HabitResponse.of(habit, new StreakResult(0, false, null));
    }

    @Transactional(readOnly = true)
    public HabitListResponse list(Long userId) {
        LocalDate today = LocalDate.now(clock);
        List<Habit> habits = habitRepository.findAllByUserIdOrderByIdAsc(userId);
        Set<Long> todayChecked = todayChecked(userId, today);
        List<HabitResponse> items = habits.stream()
                .map(habit -> toResponse(habit, userId, today, todayChecked))
                .toList();
        return new HabitListResponse(today, items);
    }

    private Set<Long> todayChecked(Long userId, LocalDate today) {
        return cache.getTodayChecked(userId, today)
                .orElseGet(() -> {
                    Set<Long> ids = recordRepository.findCheckedHabitIds(userId, today);
                    cache.putTodayChecked(userId, today, ids);
                    return ids;
                });
    }

    private HabitResponse toResponse(Habit habit, Long userId, LocalDate today, Set<Long> todayChecked) {
        StreakResult streak = cache.getStreak(userId, habit.getId(), today)
                .orElseGet(() -> {
                    Set<LocalDate> dates = recordRepository.findDates(userId, habit.getId(), today);
                    StreakResult s = streakService.calculate(dates, today);
                    cache.putStreak(userId, habit.getId(), today, s);
                    return s;
                });
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getCreatedAt(),
                todayChecked.contains(habit.getId()),
                streak.streak(),
                streak.streakAsOf());
    }
}