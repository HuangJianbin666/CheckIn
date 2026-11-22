package com.example.checkin.auth;

import com.example.checkin.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer token 认证过滤器。Redis 会话异常时终止过滤链并返回 503，绝不放行。
 */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    private final RedisSessionStore sessionStore;

    public BearerTokenFilter(RedisSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            try {
                Long userId = sessionStore.find(token);
                if (userId == null) {
                    reject(response, ErrorCode.UNAUTHENTICATED);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (RedisConnectionFailureException e) {
                reject(response, ErrorCode.AUTH_SERVICE_UNAVAILABLE);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"code\":\"" + code.code() + "\",\"message\":\"" + code.message() + "\",\"data\":null}");
    }
}