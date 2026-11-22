package com.example.checkin.auth;

import com.example.checkin.common.ApiException;
import com.example.checkin.common.ErrorCode;
import com.example.checkin.user.User;
import com.example.checkin.user.UserRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisSessionStore sessionStore;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RedisSessionStore sessionStore) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionStore = sessionStore;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User(username, passwordEncoder.encode(request.password()));
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 并发注册同名时唯一键冲突
            throw new ApiException(ErrorCode.USERNAME_EXISTS);
        }
        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        String token = newToken();
        sessionStore.save(token, user.getId());
        return new LoginResponse(token, UserResponse.from(user));
    }

    public UserResponse authenticate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        return UserResponse.from(user);
    }

    public void logout(String token) {
        sessionStore.delete(token);
    }

    private String normalizeUsername(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}