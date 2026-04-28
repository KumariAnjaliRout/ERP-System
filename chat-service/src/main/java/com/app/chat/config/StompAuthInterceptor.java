package com.app.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            //  DO NOT throw if missing
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return message;
            }

            try {

                String token = authHeader.substring(7);

                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                String orgId = jwtUtil.extractOrganizationId(token);
                String outletId = jwtUtil.extractOutletId(token);

                CustomUserPrincipal principal =
                        new CustomUserPrincipal(userId, role, orgId, outletId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);

            } catch (Exception e) {

                // ✅ invalid token → reject
                throw new RuntimeException("Invalid JWT in WebSocket");
            }
        }
        return message;
    }
}