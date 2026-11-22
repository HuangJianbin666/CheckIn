package com.example.checkin.habit;

import com.example.checkin.checkin.StreakResult;
import java.time.Instant;
import java.time.LocalDate;

public record HabitResponse(
        Long id,
        String name,
        Instant createdAt,
        boolean todayChecked,
        int streak,
        LocalDate streakAsOf) {

    public static HabitResponse of(Habit habit, StreakResult streak) {
        return new HabitResponse(
                habit.getId(),
                habit.getName(),
                habit.getCreatedAt(),
                streak.todayChecked(),
                streak.streak(),
                streak.streakAsOf());
    }
}