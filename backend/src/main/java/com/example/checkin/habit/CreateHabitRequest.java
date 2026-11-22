package com.example.checkin.habit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHabitRequest(
        @NotBlank
        @Size(min = 1, max = 50, message = "名称长度需为 1-50 字")
        String name) {
}