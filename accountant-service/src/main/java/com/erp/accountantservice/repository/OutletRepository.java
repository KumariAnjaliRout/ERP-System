package com.erp.accountantservice.repository;

import com.erp.accountantservice.entity.Outlet;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, UUID> {
    List<Outlet> findByAccountantId(@Param("accountantId") String accountantId);
    List<Outlet> findByOrganizationId(String organizationId);
}
