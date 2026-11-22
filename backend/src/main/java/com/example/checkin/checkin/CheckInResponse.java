package com.example.checkin.checkin;

import java.time.LocalDate;

public record CheckInResponse(
        Long recordId,
        Long habitId,
        LocalDate checkInDate,
        boolean todayChecked,
        int streak,
        LocalDate streakAsOf) {

    public static CheckInResponse of(CheckInRecordRow record, StreakResult streak) {
        return new CheckInResponse(
                record.id(),
                record.habitId(),
                record.checkInDate(),
                streak.todayChecked(),
                streak.streak(),
                streak.streakAsOf());
    }
}