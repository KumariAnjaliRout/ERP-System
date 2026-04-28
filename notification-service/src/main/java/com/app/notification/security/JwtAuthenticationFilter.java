package com.app.notification.security;

import com.app.notification.dto.CustomPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ VERY IMPORTANT — skip async dispatch
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Avoid re-authentication
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = UUID.fromString(jwtService.extractUserId(token));
        String role = jwtService.extractRole(token);
        String organizationId = jwtService.extractOrganizationId(token);
        String outletId = jwtService.extractOutletId(token);

        CustomPrincipal principal =
                new CustomPrincipal(userId, role, organizationId, outletId);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        token,
                        principal.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String authHeader = request.getHeader("Authorization");
//
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request,response);
//            return;
//        }
//
//        String token = authHeader.substring(7);
//
//        if (!jwtService.validateToken(token)) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        UUID userId = UUID.fromString(jwtService.extractUserId(token));
//        String role = jwtService.extractRole(token);
//        String organizationId = jwtService.extractOrganizationId(token);
//        String outletId = jwtService.extractOutletId(token);
//
//        CustomPrincipal principal =
//                new CustomPrincipal(userId, role, organizationId, outletId);
//
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(
//                        principal,
//                        token, // store token for Feign forwarding
//                        principal.getAuthorities()
//                );
//
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        filterChain.doFilter(request, response);
//    }

}
