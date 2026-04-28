package com.erp.erpsystem.service;

import com.erp.erpsystem.entity.RefreshToken;
import com.erp.erpsystem.entity.User;
import com.erp.erpsystem.exception.UnauthorizedException;
import com.erp.erpsystem.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;


    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;


    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                // Secure random UUID as token string — not guessable
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token created for user: {}", user.getId());
        return saved;
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found: {}", token);
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (refreshToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token for user: {}",
                    refreshToken.getUser().getId());
            revokeAllUserTokens(refreshToken.getUser());
            throw new UnauthorizedException("Refresh token has been revoked. Please login again.");
        }

        if (refreshToken.isExpired()) {
            log.warn("Expired refresh token used for user: {}", refreshToken.getUser().getId());
            refreshTokenRepository.revokeByToken(token);
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresentOrElse(
                rt -> {
                    refreshTokenRepository.revokeByToken(token);
                    log.debug("Refresh token revoked for user: {}", rt.getUser().getId());
                },
                () -> log.warn("Attempted to revoke non-existent token: {}", token)
        );
    }


    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("All refresh tokens revoked for user: {}", user.getId());
    }


    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running scheduled refresh token cleanup...");
        refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
        log.info("Refresh token cleanup complete.");
    }
}