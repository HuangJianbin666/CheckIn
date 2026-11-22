package com.example.checkin.auth;

public record LoginResponse(String token, UserResponse user) {
}