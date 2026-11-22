package com.example.checkin.checkin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 打卡记录读写。写入使用原子 upsert 保证并发幂等，读取直接走 JdbcTemplate 以满足
 * {@link CheckInWriter} 独立事务内的写后读一致性。
 */
@Repository
public class CheckInRecordJdbcRepository {

    private static final String UPSERT = """
            INSERT INTO check_in_records (user_id, habit_id, check_in_date)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE id = id
            """;

    private static final String SELECT_BY_KEY = """
            SELECT id, user_id, habit_id, check_in_date, created_at
            FROM check_in_records
            WHERE user_id = ? AND habit_id = ? AND check_in_date = ?
            """;

    private static final String SELECT_DATES = """
            SELECT check_in_date
            FROM check_in_records
            WHERE user_id = ? AND habit_id = ? AND check_in_date <= ?
            ORDER BY check_in_date DESC
            """;

    private static final String SELECT_CHECKED_HABIT_IDS = """
            SELECT DISTINCT habit_id
            FROM check_in_records
            WHERE user_id = ? AND check_in_date = ?
            """;

    private final JdbcTemplate jdbc;

    public CheckInRecordJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsert(Long userId, Long habitId, LocalDate date) {
        jdbc.update(UPSERT, userId, habitId, date);
    }

    public Optional<CheckInRecordRow> findByKey(Long userId, Long habitId, LocalDate date) {
        List<CheckInRecordRow> rows = jdbc.query(SELECT_BY_KEY,
                (rs, i) -> new CheckInRecordRow(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("habit_id"),
                        rs.getObject("check_in_date", LocalDate.class),
                        rs.getTimestamp("created_at").toInstant()),
                userId, habitId, date);
        return rows.stream().findFirst();
    }

    public Set<LocalDate> findDates(Long userId, Long habitId, LocalDate upTo) {
        List<LocalDate> dates = jdbc.query(SELECT_DATES,
                (rs, i) -> rs.getObject("check_in_date", LocalDate.class),
                userId, habitId, upTo);
        return dates.stream().collect(Collectors.toUnmodifiableSet());
    }

    public Set<Long> findCheckedHabitIds(Long userId, LocalDate date) {
        List<Long> ids = jdbc.query(SELECT_CHECKED_HABIT_IDS,
                (rs, i) -> rs.getLong("habit_id"),
                userId, date);
        return ids.stream().collect(Collectors.toUnmodifiableSet());
    }
}