package com.example.checkin.checkin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, Long> {

    Optional<CheckInRecord> findByUserIdAndHabitIdAndCheckInDate(Long userId, Long habitId, LocalDate date);

    List<CheckInRecord> findAllByUserIdAndHabitId(Long userId, Long habitId);

    boolean existsByUserIdAndHabitIdAndCheckInDate(Long userId, Long habitId, LocalDate date);
}