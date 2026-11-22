package com.example.checkin.checkin;

import com.example.checkin.common.ApiException;
import com.example.checkin.common.ErrorCode;
import com.example.checkin.habit.HabitRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 打卡写入的独立事务 Bean。使用 READ_COMMITTED + 原子 upsert 保证并发幂等。
 * 唯一键冲突由 upsert 语义消化：前序事务提交则本次变为 no-op，回滚则本次正常插入。
 */
@Service
public class CheckInWriter {

    private final CheckInRecordJdbcRepository repository;
    private final HabitRepository habitRepository;
    private final Clock clock;

    public CheckInWriter(CheckInRecordJdbcRepository repository, HabitRepository habitRepository,
                         Clock clock) {
        this.repository = repository;
        this.habitRepository = habitRepository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckInRecordRow write(Long userId, Long habitId) {
        boolean owned = habitRepository.findByIdAndUserId(habitId, userId).isPresent();
        if (!owned) {
            throw new ApiException(ErrorCode.HABIT_NOT_FOUND);
        }
        LocalDate today = LocalDate.now(clock);
        repository.upsert(userId, habitId, today);
        return repository.findByKey(userId, habitId, today)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
    }
}