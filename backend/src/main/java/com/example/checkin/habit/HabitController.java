package com.example.checkin.habit;

import com.example.checkin.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateHabitRequest request) {
        HabitResponse habit = habitService.create(userId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(habit));
    }

    @GetMapping
    public ApiResponse<HabitListResponse> list(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(habitService.list(userId));
    }
}