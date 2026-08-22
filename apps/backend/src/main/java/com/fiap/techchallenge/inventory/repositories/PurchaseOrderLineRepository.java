package com.fiap.techchallenge.inventory.repositories;

import com.fiap.techchallenge.inventory.entities.PurchaseOrderLine;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, UUID> {

    /**
     * What's ordered but not yet on the shelf for this part, across every purchase order still in
     * one of {@code openStatuses} — the "inbound" term in inventory position.
     */
    @Query("""
            select coalesce(sum(l.quantityOrdered - l.quantityReceived), 0)
            from PurchaseOrderLine l
            where l.part.id = :partId
              and l.purchaseOrder.status in :openStatuses
            """)
    BigDecimal sumInboundForPart(@Param("partId") UUID partId, @Param("openStatuses") Collection<PurchaseOrderStatus> openStatuses);
}
