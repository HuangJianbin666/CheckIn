package com.example.checkin.checkin;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

/**
 * 连续天数算法。纯函数，不访问时钟与数据库，日期由调用方注入以便测试。
 */
@Service
public class StreakService {

    /**
     * 依据已打卡日期集合与"今日"计算连续天数。
     *
     * @param checkInDates 该用户该打卡项的打卡日期集合（忽略晚于 today 的日期）
     * @param today        服务端业务时区下的今日
     */
    public StreakResult calculate(Set<LocalDate> checkInDates, LocalDate today) {
        TreeSet<LocalDate> sorted = new TreeSet<>(Comparator.reverseOrder());
        for (LocalDate date : checkInDates) {
            if (date != null && !date.isAfter(today)) {
                sorted.add(date);
            }
        }
        if (sorted.isEmpty()) {
            return new StreakResult(0, false, null);
        }

        LocalDate latest = sorted.first();
        LocalDate yesterday = today.minusDays(1);
        if (latest.isBefore(yesterday)) {
            return new StreakResult(0, false, null);
        }

        boolean todayChecked = latest.equals(today);
        LocalDate anchor = todayChecked ? today : yesterday;

        int streak = 0;
        LocalDate cursor = anchor;
        while (sorted.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return new StreakResult(streak, todayChecked, anchor);
    }
}