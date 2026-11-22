package com.example.checkin.checkin;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 用 JdbcTemplate 映射的打卡记录视图，仅用于仓储查询结果。
 */
public record CheckInRecordRow(
        Long id, Long userId, Long habitId, LocalDate checkInDate, Instant createdAt) {
}