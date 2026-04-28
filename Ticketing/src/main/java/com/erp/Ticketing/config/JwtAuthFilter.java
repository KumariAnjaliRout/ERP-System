package com.erp.Ticketing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;


import java.io.IOException;
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.info("JWT Filter executing for: {}", request.getRequestURI());

        String authHeader = request.getHeader("Authorization");

        log.info("Authorization Header = {}", authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                System.out.println("Token is valid");
                String userId = jwtUtil.extractUserId(token);
                System.out.println("UserID = " + userId);
                String role = jwtUtil.extractRole(token);
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }
                System.out.println("Role = " + role);
                String organizationId = jwtUtil.extractOrganizationId(token);
                String outletId = jwtUtil.extractOutletId(token);
                CustomUserPrincipal principal = new CustomUserPrincipal(
                        userId,
                        role,
                        organizationId,
                        outletId
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                Collections.singletonList(
                                        new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                )
                        );
                log.info("Role from token = {}", role);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authentication set in SecurityContext");
                log.info("Authorities inside filter: {}",
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getAuthorities());

            }
        }
        filterChain.doFilter(request, response);
    }
}