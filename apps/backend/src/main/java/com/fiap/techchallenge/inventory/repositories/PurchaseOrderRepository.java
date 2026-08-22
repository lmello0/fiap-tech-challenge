package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    @Query(value = "SELECT nextval('inventory.seq_purchase_order_code')", nativeQuery = true)
    Long getNextSequence();
}
