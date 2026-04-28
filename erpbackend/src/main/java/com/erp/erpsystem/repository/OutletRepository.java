package com.erp.erpsystem.repository;

import com.erp.erpsystem.entity.Outlet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, String> {

    List<Outlet> findByOrganizationId(String organizationId);

    Page<Outlet> findByOrganizationId(String organizationId, Pageable pageable);

    // Replaces findByOrganizationIdNative — JPQL is sufficient here
    @Query("SELECT o FROM Outlet o WHERE o.organizationId = :orgId")
    Page<Outlet> findByOrganizationIdNative(@Param("orgId") String orgId, Pageable pageable);

    // Bulk existence check — avoids N+1 in validateOutletsForFeign
    @Query("SELECT o FROM Outlet o WHERE o.id IN :ids")
    List<Outlet> findAllByIdIn(@Param("ids") Collection<String> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM Outlet o WHERE o.organizationId = :orgId")
    void deleteByOrganizationId(@Param("orgId") String orgId);

    @Modifying
    @Query("UPDATE Outlet o SET o.isActive = :active WHERE o.organizationId = :organizationId")
    void updateActiveStatusByOrganizationId(
            @Param("organizationId") String organizationId,
            @Param("active") boolean active);
}