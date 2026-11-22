package com.example.checkin.checkin;

import java.time.LocalDate;

/**
 * 连续天数的计算结果。
 *
 * @param streak       当前连续天数
 * @param todayChecked 今日是否已打卡
 * @param streakAsOf   当前连续段的锚点日期（今日或昨日），无有效连续段时为 null
 */
public record StreakResult(int streak, boolean todayChecked, LocalDate streakAsOf) {
}