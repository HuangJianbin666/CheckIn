package com.example.checkin.checkin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "check_in_records")
public class CheckInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "habit_id", nullable = false)
    private Long habitId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected CheckInRecord() {
    }

    public CheckInRecord(Long userId, Long habitId, LocalDate checkInDate) {
        this.userId = userId;
        this.habitId = habitId;
        this.checkInDate = checkInDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getHabitId() {
        return habitId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}