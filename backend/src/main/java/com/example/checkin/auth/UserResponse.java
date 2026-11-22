package com.example.checkin.auth;

import com.example.checkin.user.User;
import java.time.Instant;

public record UserResponse(Long id, String username, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}