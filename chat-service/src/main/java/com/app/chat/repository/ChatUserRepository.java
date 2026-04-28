package com.app.chat.repository;

import com.app.chat.entity.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ChatUserRepository extends JpaRepository<ChatUser, UUID> {


    List<ChatUser> findByOrganizationIdAndRoleInAndUserIdNot(
            String organizationId,
            List<String> roles,
            UUID userId
    );

    List<ChatUser> findByOrganizationIdAndUserIdNot(
            String organizationId,
            UUID userId
    );

    List<ChatUser> findByRoleInAndUserIdNot(
            List<String> roles,
            UUID userId
    );
    List<ChatUser> findByUserIdNot(UUID userId);



    Page<ChatUser> findByUserIdNot(UUID userId, Pageable pageable);



    @Query("""
SELECT u FROM ChatUser u
WHERE u.userId != :requesterId
AND u.role <> com.app.chat.entity.Role.OUTLET
AND (
    u.organizationId = :orgId
    OR u.role IN (
        com.app.chat.entity.Role.SUPER_ADMIN,
        com.app.chat.entity.Role.SUPER_ACCOUNTANT,
        com.app.chat.entity.Role.ADMIN
    )
)
""")
    Page<ChatUser> findUsersVisibleToAdmin(
            UUID requesterId,
            String orgId,
            Pageable pageable
    );




    @Query("""
SELECT u FROM ChatUser u
WHERE u.userId != :requesterId
AND u.role <> com.app.chat.entity.Role.OUTLET
""")
    Page<ChatUser> findAllVisibleToSuper(
            UUID requesterId,
            Pageable pageable
    );

    @Query("""
SELECT u FROM ChatUser u
WHERE u.userId != :requesterId
AND u.role <> com.app.chat.entity.Role.OUTLET
AND u.organizationId = :orgId
""")
    Page<ChatUser> findSameOrgUsers(
            UUID requesterId,
            String orgId,
            Pageable pageable
    );

}

