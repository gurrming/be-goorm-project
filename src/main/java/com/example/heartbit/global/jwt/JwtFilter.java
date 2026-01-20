package com.example.heartbit.global.jwt;

import com.example.heartbit.domain.Member;
import com.example.heartbit.global.exception.CustomerException;
import com.example.heartbit.global.exception.ErrorCode;
import com.example.heartbit.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (token != null && jwtTokenProvider.validateToken(token)) {

                String userId = jwtTokenProvider.getSubject(token);
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));

                UserDetails principal = new User(userId, "", authorities);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        principal, null
                        , Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (CustomerException e){
            log.warn("JWT Filter Exception: {}", e.getErrorCode().getMessage());
            jwtExceptionHandler(response, e.getErrorCode());
        }
    }
    public void jwtExceptionHandler(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // ApiResponse.onFailure() 형태의 JSON 문자열 생성
            String json = new ObjectMapper().writeValueAsString(
                    ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage())
            );
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String[] excludedPaths = {
                "/api/member/signup",
                "/api/member/login",
                "/api/chatroom/",
                "/api/orders/",
                "/api/orders/orderbook",
                "/api/trades/chart",
                "/api/categories",
                "/api/category",
                "/ws-heartbit",
                "/h2-console",
                "/favicon.ico"
        };

        for (String excludedPath : excludedPaths) {
            if (path.startsWith(excludedPath)) {
                return true; // 이 경로는 필터 실행 건너뛰기
            }
        }
        return false;
    }
}
