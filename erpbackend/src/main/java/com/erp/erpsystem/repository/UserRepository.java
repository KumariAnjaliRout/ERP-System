package com.erp.erpsystem.repository;

import com.erp.erpsystem.entity.Role;
import com.erp.erpsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByOrganizationId(String organizationId);
    List<User> findByOrganizationIdAndRole(String organizationId, Role role);
    List<User> findByRole(Role role);
    boolean existsByRole(Role role);
    boolean existsByOrganizationIdAndRole(String organizationId, Role role);
    boolean existsByOutletId(String outletId);
    Page<User> findByOrganizationIdAndIsActive(String organizationId, boolean isActive, Pageable pageable);
    Page<User> findByIsActive(boolean isActive, Pageable pageable);
    List<User> findByCreatedBy(UUID createdBy);
    Page<User> findAllByOrganizationId(String organizationId, Pageable pageable);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByOrganizationIdAndRole(String organizationId, Role role, Pageable pageable);
    boolean existsByOutletIdAndIsActiveTrue(String outletId);
    @Override
    @NonNull
    Page<User> findAll(@NonNull Pageable pageable);

    @Query("SELECT u FROM User u ORDER BY LOWER(u.email) ASC")
    Page<User> findAllOrderByEmailAsc(Pageable pageable);

    @Query("SELECT u FROM User u ORDER BY LOWER(u.email) DESC")
    Page<User> findAllOrderByEmailDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.organizationId = :orgId")
    void deleteByOrganizationId(@Param("orgId") String orgId);

    boolean existsByOrganizationIdAndRoleAndIdNot(String organizationId, Role role, UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.organizationId = :orgId")
    int updateActiveStatusByOrganizationId(
            @Param("orgId") String orgId,
            @Param("isActive") boolean isActive);
}