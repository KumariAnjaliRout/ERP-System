package com.erp.Ticketing.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz

                        .requestMatchers(HttpMethod.GET, "/api/tickets/**")
                        .hasAnyRole("ADMIN","EMPLOYEE","SUPER_ADMIN","HR","MANAGER","ACCOUNTANT","OUTLET","SUPER_ACCOUNTANT")

                        .requestMatchers("/api/tickets/raise")
                        .hasAnyRole("HR","EMPLOYEE", "MANAGER", "ACCOUNTANT", "OUTLET","ADMIN","SUPER_ACCOUNTANT")

                        .requestMatchers(HttpMethod.POST,"/api/tickets/*/escalate")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/tickets/*/status/*")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .requestMatchers("/api/tickets/test-auth").permitAll()
                        .requestMatchers("/h2-console/**", "/actuator/**").permitAll() // just for testing
                        .requestMatchers("/error").permitAll()

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}