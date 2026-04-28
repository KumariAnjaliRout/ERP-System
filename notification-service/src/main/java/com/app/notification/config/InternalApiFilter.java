package com.app.notification.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalApiFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String expectedSecret;

    private static final String INTERNAL_HEADER = "X-Internal-Secret";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only protect internal endpoints
        if (path.startsWith("/internal/")) {

            String headerSecret = request.getHeader(INTERNAL_HEADER);

            if (headerSecret == null || !headerSecret.equals(expectedSecret)) {
                throw new AccessDeniedException("Invalid internal API secret");
            }
        }

        filterChain.doFilter(request, response);
    }
}