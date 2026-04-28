package com.erp.erpsystem.repository;

import com.erp.erpsystem.entity.RefreshToken;
import com.erp.erpsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Find by token string
    Optional<RefreshToken> findByToken(String token);

    // Revoke a specific token
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    void revokeByToken(@Param("token") String token);

    // Revoke all tokens for a user (used on password change)
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(@Param("user") User user);

    // Cleanup job — delete expired or revoked tokens older than a threshold
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true OR rt.expiryDate < :threshold")
    void deleteExpiredOrRevoked(@Param("threshold") Instant threshold);

    // Check if a valid (not revoked, not expired) token exists for user
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.user = :user " +
            "AND rt.revoked = false AND rt.expiryDate > :now")
    boolean hasValidTokenForUser(@Param("user") User user, @Param("now") Instant now);
}