package com.example.heartbit.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServerTokenFilter extends OncePerRequestFilter {

    @Value("${server.secret-token}")
    private String serverToken;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("X-Internal-Token");

        if(authHeader != null){
            String token = authHeader.toString();
            if(serverToken.equals(token)){
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        "BOT", null, List.of()
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request,response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String[] excludedPaths = {
                "/api/member/signup",
                "/api/member/login",
                "/ws-heartbit",
                "/favicon.ico"
        };

        // 경로가 excludedPaths에 포함되면 필터 실행 안 함 (true 반환)
        for (String excludedPath : excludedPaths) {
            if (path.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

}
