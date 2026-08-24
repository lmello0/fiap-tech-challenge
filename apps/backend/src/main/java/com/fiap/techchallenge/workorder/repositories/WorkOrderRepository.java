package com.fiap.techchallenge.workorder.repositories;

import com.fiap.techchallenge.workorder.api.representation.WorkOrderCountStatusInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {
    @Query(value = "SELECT nextval('work_orders.seq_work_order_code')", nativeQuery = true)
    Long getNextSequence();

    @Query("""
        SELECT new com.fiap.techchallenge.workorder.api.representation.WorkOrderCountStatusInfo(
            COALESCE(SUM(CASE WHEN wo.status = 'RECEIVED'            THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'WAITING_DIAGNOSTICS' THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'IN_DIAGNOSTICS'      THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'BUDGET_IN_DRAFT'     THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'WAITING_APPROVAL'    THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'APPROVED'            THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'REFUSED'             THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'IN_PROGRESS'         THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'FINISHED'            THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'WAITING_PICKUP'      THEN 1L ELSE 0L END), 0L),
            COALESCE(SUM(CASE WHEN wo.status = 'DELIVERED'           THEN 1L ELSE 0L END), 0L)
        )
        FROM WorkOrder wo
        WHERE wo.createdAt BETWEEN :start AND :end
    """)
    WorkOrderCountStatusInfo countByStatus(
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
