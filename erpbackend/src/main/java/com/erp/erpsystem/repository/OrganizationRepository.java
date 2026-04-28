package com.erp.erpsystem.repository;

import com.erp.erpsystem.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

}