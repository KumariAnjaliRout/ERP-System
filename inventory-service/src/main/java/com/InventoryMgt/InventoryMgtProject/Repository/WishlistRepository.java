package com.InventoryMgt.InventoryMgtProject.Repository;

import com.InventoryMgt.InventoryMgtProject.Entities.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface WishlistRepository extends JpaRepository<WishList, Long> {

    Optional<WishList> findByOutletId(String outletId);

}