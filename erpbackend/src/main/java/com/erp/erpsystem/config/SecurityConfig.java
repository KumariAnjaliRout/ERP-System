package com.erp.erpsystem.config;

import com.erp.erpsystem.security.JwtAuthenticationFilter;
import com.erp.erpsystem.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("""
                ROLE_SUPER_ADMIN > ROLE_SUPER_ACCOUNTANT
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_MANAGER
                ROLE_ADMIN > ROLE_HR
                ROLE_ADMIN > ROLE_ACCOUNTANT
                ROLE_MANAGER > ROLE_OUTLET
                ROLE_HR > ROLE_EMPLOYEE
                """);
        return hierarchy;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Access Denied\", " +
                                            "\"message\": \"You don't have permission to access this resource\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh-token").permitAll()
                        .requestMatchers("/internal/**").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/init/super-admin").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()

                        // ── ACTUATOR (admin only) ─────────────────────────────
                        .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")

                        // ── AUTH ──────────────────────────────────────────────
                        .requestMatchers("/api/auth/change-password").authenticated()
                        .requestMatchers("/api/auth/create-admin").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/auth/create-super-accountant").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/auth/create-user")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "HR")

                        // ── USERS ─────────────────────────────────────────────

                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/email/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/my-organization")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/role/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/filter/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/change-password").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/role").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/reset-password").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/org-reset-password").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/activation")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "HR").requestMatchers(HttpMethod.GET, "/api/users").hasRole("SUPER_ADMIN").requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN").requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN").requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")                        // ── ORGANIZATIONS ─────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/organizations")
                        .hasAnyRole("SUPER_ADMIN", "SUPER_ACCOUNTANT")
                        .requestMatchers(HttpMethod.GET, "/api/organizations/**")
                        .hasAnyRole("SUPER_ADMIN", "SUPER_ACCOUNTANT", "ADMIN", "OUTLET")
                        .requestMatchers(HttpMethod.POST, "/api/organizations").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/**").hasRole("SUPER_ADMIN")

                        // ── OUTLETS ───────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/outlets")
                        .hasAnyRole("SUPER_ADMIN", "SUPER_ACCOUNTANT")
                        .requestMatchers(HttpMethod.GET, "/api/outlets/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "OUTLET", "ACCOUNTANT", "SUPER_ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/api/outlets")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/outlets/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER", "OUTLET")
                        .requestMatchers(HttpMethod.DELETE, "/api/outlets/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // ── AUDIT ─────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/audit/**")
                        .hasAnyRole("SUPER_ADMIN", "SUPER_ACCOUNTANT","ADMIN")

                        // ── FALLBACK ──────────────────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.0.149:*"
        ));
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


