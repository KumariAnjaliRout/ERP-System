package com.erp.erpsystem.security;

import com.erp.erpsystem.service.JwtService;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static final Set<String> ROLES_REQUIRING_ORG_ID = Set.of(
            "ROLE_ADMIN",
            "ROLE_MANAGER",
            "ROLE_HR",
            "ROLE_ACCOUNTANT",
            "ROLE_OUTLET",
            "ROLE_EMPLOYEE"
    );

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        return path.equals("/api/auth/login") ||
                // FIX: refresh-token must skip JWT filter — access token may be expired
                path.equals("/api/auth/refresh-token") ||
                path.equals("/init/super-admin") ||
                path.equals("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateToken(token)) {
            log.warn("Token validation failed for: {}", request.getRequestURI());
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        String role = jwtService.extractRole(token);
        String userId = jwtService.extractUserId(token);
        String organizationId = jwtService.extractOrganizationId(token);
        // FIX: extract outletId and put it in details map
        String outletId = jwtService.extractOutletId(token);

        if (role == null || role.isEmpty() || userId == null) {
            log.warn("Missing required claims (role/userId) in token for URI: {}",
                    request.getRequestURI());
            writeUnauthorized(response, "Token missing required claims");
            return;
        }

        if (organizationId == null && ROLES_REQUIRING_ORG_ID.contains(role)) {
            log.warn("Missing organizationId in token for role: {} on URI: {}",
                    role, request.getRequestURI());
            writeUnauthorized(response, "Token missing required claims");
            return;
        }

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        details.put("role", role);
        details.put("organizationId", organizationId);
        details.put("outletId", outletId);

        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }


    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }


    private void writeUnauthorized(HttpServletResponse response,
                                   String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"" + safeMessage + "\"}"
        );
    }
}