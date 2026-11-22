package com.example.checkin.habit;

import java.time.LocalDate;
import java.util.List;

public record HabitListResponse(LocalDate today, List<HabitResponse> items) {
}