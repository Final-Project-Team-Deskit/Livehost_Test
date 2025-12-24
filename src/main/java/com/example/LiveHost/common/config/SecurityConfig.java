package com.example.LiveHost.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 보안 끄기 (REST API는 보통 끕니다)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS 허용 (프론트엔드 연동 위해)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 모든 API 요청 허용 (개발 중이니까!)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("seller/api/**").permitAll() // /api로 시작하는 건 다 통과
                        .requestMatchers("admin/api/**").permitAll()
                        .anyRequest().authenticated() // 그 외는 인증 필요
                );

        return http.build();
    }

    // CORS 설정 (나중에 프론트엔드 포트 5173 등 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 모든 출처 허용 (보안상 나중에 프론트 주소로 변경 필요)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}