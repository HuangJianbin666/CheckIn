package com.example.checkin.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9_]{3,32}", message = "用户名需为 3-32 位字母、数字或下划线")
        String username,

        @NotBlank
        @Size(min = 8, max = 72, message = "密码长度需为 8-72 位")
        String password) {
}