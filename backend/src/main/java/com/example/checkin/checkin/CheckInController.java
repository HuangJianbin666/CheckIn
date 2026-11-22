package com.example.checkin.checkin;

import com.example.checkin.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/habits")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/{habitId}/check-ins")
    public ApiResponse<CheckInResponse> checkIn(@AuthenticationPrincipal Long userId,
                                                @PathVariable Long habitId) {
        return ApiResponse.ok(checkInService.checkIn(userId, habitId));
    }
}