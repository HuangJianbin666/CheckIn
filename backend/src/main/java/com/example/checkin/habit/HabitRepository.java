package com.example.checkin.habit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<Habit> findByIdAndUserId(Long id, Long userId);
}