package com.fiap.techchallenge.workorder.repositories;

import com.fiap.techchallenge.workorder.entities.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {
    @Query(value = "SELECT nextval('work_orders.seq_work_order_code')", nativeQuery = true)
    Long getNextSequence();
}
