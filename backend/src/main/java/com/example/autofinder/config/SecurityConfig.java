package com.example.autofinder.config;

import com.example.autofinder.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
                    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    configuration.setAllowedHeaders(Arrays.asList("*"));
                    configuration.setAllowCredentials(true);
                    return configuration;
                }))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 접근 허용 (인증 불필요)
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/cars/**").permitAll()
                        .requestMatchers("/api/comparison/**").permitAll()  // 비교 기능 공개 접근
                        .requestMatchers("/api/analytics/**").permitAll()   // 분석 기능 공개 접근
                        .requestMatchers("/api/system/status").permitAll()  // 시스템 상태 공개 접근
                        .requestMatchers("/api/system/health").permitAll()  // 헬스체크 공개 접근

                        // 인증 필요 (로그인 사용자만)
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/api/ai/**").authenticated()        // AI 추천은 로그인 필요
                        .requestMatchers("/api/behavior/**").authenticated()  // 사용자 행동 추적은 로그인 필요
                        .requestMatchers("/api/auth/me").authenticated()      // 사용자 정보 조회는 로그인 필요

                        // 관리자 권한 필요 (추후 확장용)
                        .requestMatchers("/api/system/ai/retrain").hasRole("ADMIN")      // AI 재학습은 관리자만
                        .requestMatchers("/api/system/cache/clear").hasRole("ADMIN")     // 캐시 정리는 관리자만
                        .requestMatchers("/api/comparison/stats").hasRole("ADMIN")       // 비교 통계는 관리자만

                        // 나머지는 모두 허용
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // JWT 필터 추가

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());  // 암호화된 비밀번호 비교
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}