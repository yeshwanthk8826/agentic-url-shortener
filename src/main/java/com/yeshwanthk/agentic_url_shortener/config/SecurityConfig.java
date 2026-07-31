package com.yeshwanthk.agentic_url_shortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/urls/**",
                                "/actuator/health",
                                "/api/v1/workflows/**",
                                "/api/v1/workflows/**",
                                "/actuator/health/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/*").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}