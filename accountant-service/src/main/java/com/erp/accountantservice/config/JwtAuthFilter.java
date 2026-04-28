package com.erp.accountantservice.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Collections;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthFilter extends OncePerRequestFilter {
//
//    private final JwtUtil jwtUtil;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String authHeader = request.getHeader("Authorization");
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//
//            String token = authHeader.substring(7);
//
//            if (jwtUtil.isTokenValid(token)) {
//
//                String userId = jwtUtil.extractUserId(token);
//                String role = jwtUtil.extractRole(token);
//
//                if (!role.startsWith("ROLE_")) {
//                    role = "ROLE_" + role;
//                }
//
//                String organizationId =
//                        jwtUtil.extractOrganizationId(token);
//
//                String outletId =
//                        jwtUtil.extractOutletId(token);
//
//                CustomUserPrincipal principal =
//                        new CustomUserPrincipal(
//                                userId,
//                                role,
//                                organizationId,
//                                outletId
//                        );
//
//                UsernamePasswordAuthenticationToken auth =
//                        new UsernamePasswordAuthenticationToken(
//                                principal,
//                                null,
//                                Collections.singletonList(
//                                        new SimpleGrantedAuthority(role)
//                                )
//                        );
//
//                SecurityContextHolder.getContext()
//                        .setAuthentication(auth);
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;


import java.io.IOException;
import java.util.Collections;

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

        String authHeader = request.getHeader("Authorization");
        try{
            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                if (jwtUtil.isTokenValid(token)) {

                    String userId = jwtUtil.extractUserId(token);
                    String role = jwtUtil.extractRole(token);
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
                                            new SimpleGrantedAuthority(role)
                                    )
                            );


                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }}catch (Exception e){
            response.sendError(401,"Token invalid");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
