package com.example.heartbit.config;

import com.example.heartbit.global.jwt.JwtFilter;
import com.example.heartbit.global.jwt.JwtTokenProvider;
import com.example.heartbit.global.security.ServerTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final ServerTokenFilter serverTokenFilter;

    // "이 URL은 로그인 안 한 사람(Anonymous)도 들어와도 좋다."
    public static final String[] ALLOWED_URLS = {
            "/api/member/signup",
            "/api/member/login",
            "/api/member/exists",
            "/api/member/reissue",
            "/v3/api-docs/**",
            "/api/orders/**",
            "/api/chatroom/**",
            "/api/orders/orderbook",
            "/api/trades",
            "/api/trades/recent",
            "/api/trades/order/**",
            "/api/trades/volume-power/**",
            "/api/trades/chart",
            "/api/category",
            "/api/categories",
            "/api/analysis/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/ws-heartbit/**",
            "/ws-heartbit/info/**",
            "/actuator/**"
    };


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtFilter jwtFilter = new JwtFilter(jwtTokenProvider);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ALLOWED_URLS).permitAll()

                        // 로그인 안해도 채팅내역 볼 수 있게 해줌
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/chatroom/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(serverTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정 Bean 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:8080",
                        "http://localhost:5173",
                        "https://d1z2afuae81hvp.cloudfront.net",
                        "http://172.16.24.109:8080",
                        "http://3.27.95.44:8080",
                        "https://api.heartbit.site",
                        "http://*.127.0.0.1:5173"
                ));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public FilterRegistrationBean<ServerTokenFilter> preventAutoRegistration(ServerTokenFilter filter) {
        FilterRegistrationBean<ServerTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 자동 등록 비활성화
        return registration;
    }

}