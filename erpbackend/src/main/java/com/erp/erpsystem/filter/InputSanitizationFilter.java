package com.erp.erpsystem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InputSanitizationFilter extends OncePerRequestFilter {

    private static final String[] DANGEROUS_PATTERNS = {
            "../", "..\\",
            "() {", ":;};",
            "<script", "</script",
            "SELECT ", "DROP ",
            "UNION ", "INSERT ",
            "select sleep", "in (select",
            "%00", "%2e%2e",
            "owasp.org", "7970533913763005695",
            "etc/passwd", "etc/shadow",
            "http://", "https://", "www."
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI().toLowerCase();
        String queryString = request.getQueryString() != null
                ? request.getQueryString().toLowerCase() : "";

        for (String pattern : DANGEROUS_PATTERNS) {
            if (requestURI.contains(pattern.toLowerCase()) ||
                    queryString.contains(pattern.toLowerCase())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\": \"Invalid request detected\", \"status\": 400}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}