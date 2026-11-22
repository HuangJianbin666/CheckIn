package com.example.checkin.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakServiceTest {

    private final StreakService streakService = new StreakService();

    @Test
    void noRecords_isZero() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(Set.of(), today);

        assertThat(r.streak()).isZero();
        assertThat(r.todayChecked()).isFalse();
        assertThat(r.streakAsOf()).isNull();
    }

    @Test
    void onlyToday_isOne() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(Set.of(today), today);

        assertThat(r.streak()).isEqualTo(1);
        assertThat(r.todayChecked()).isTrue();
        assertThat(r.streakAsOf()).isEqualTo(today);
    }

    @Test
    void todayAndYesterday_isTwo() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(
                Set.of(today, today.minusDays(1)), today);

        assertThat(r.streak()).isEqualTo(2);
        assertThat(r.todayChecked()).isTrue();
        assertThat(r.streakAsOf()).isEqualTo(today);
    }

    @Test
    void yesterdayOnly_keepsStreakThroughYesterday() {
        LocalDate today = LocalDate.of(2026, 11, 21);
        LocalDate yesterday = today.minusDays(1);

        StreakResult r = streakService.calculate(Set.of(yesterday), today);

        assertThat(r.streak()).isEqualTo(1);
        assertThat(r.todayChecked()).isFalse();
        assertThat(r.streakAsOf()).isEqualTo(yesterday);
    }

    @Test
    void yesterdayAndDayBefore_streakThroughYesterday() {
        LocalDate today = LocalDate.of(2026, 11, 21);
        LocalDate yesterday = today.minusDays(1);

        StreakResult r = streakService.calculate(
                Set.of(yesterday, today.minusDays(2)), today);

        assertThat(r.streak()).isEqualTo(2);
        assertThat(r.todayChecked()).isFalse();
        assertThat(r.streakAsOf()).isEqualTo(yesterday);
    }

    @Test
    void gapBeforeYesterday_resetsToZero() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(
                Set.of(today.minusDays(3)), today);

        assertThat(r.streak()).isZero();
        assertThat(r.todayChecked()).isFalse();
        assertThat(r.streakAsOf()).isNull();
    }

    @Test
    void missingYesterday_breaksStreakToTodayOnly() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(
                Set.of(today, today.minusDays(2)), today);

        assertThat(r.streak()).isEqualTo(1);
        assertThat(r.todayChecked()).isTrue();
        assertThat(r.streakAsOf()).isEqualTo(today);
    }

    @Test
    void crossMonth_boundaryIsConsecutive() {
        LocalDate today = LocalDate.of(2026, 3, 1);

        StreakResult r = streakService.calculate(
                Set.of(today, LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 27)),
                today);

        assertThat(r.streak()).isEqualTo(3);
        assertThat(r.todayChecked()).isTrue();
    }

    @Test
    void crossYear_boundaryIsConsecutive() {
        LocalDate today = LocalDate.of(2026, 1, 1);

        StreakResult r = streakService.calculate(
                Set.of(today, LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 30)),
                today);

        assertThat(r.streak()).isEqualTo(3);
    }

    @Test
    void leapDay_isConsecutive() {
        LocalDate today = LocalDate.of(2024, 3, 1);

        StreakResult r = streakService.calculate(
                Set.of(today, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 28)),
                today);

        assertThat(r.streak()).isEqualTo(3);
    }

    @Test
    void futureDates_areIgnored() {
        LocalDate today = LocalDate.of(2026, 11, 21);

        StreakResult r = streakService.calculate(
                Set.of(today, today.plusDays(1)), today);

        assertThat(r.streak()).isEqualTo(1);
        assertThat(r.todayChecked()).isTrue();
    }
}