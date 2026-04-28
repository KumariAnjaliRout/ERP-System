package com.app.notification.repository;

import com.app.notification.domain.UserDevice;
import feign.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository
        extends JpaRepository<UserDevice, Long> {

    // Get active devices for push
    List<UserDevice> findByUserIdAndActiveTrue(UUID userId);

    // Safe reactivation logic
    Optional<UserDevice> findByUserIdAndDeviceToken(UUID userId,
                                                    String deviceToken);

    //  CRITICAL — Lock row during registration
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserDevice u WHERE u.deviceToken = :token")
    Optional<UserDevice> findByDeviceTokenForUpdate(@Param("token") String token);
}