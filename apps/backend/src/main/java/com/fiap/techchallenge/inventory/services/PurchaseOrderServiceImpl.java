package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderLineCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderLineCommand;
import com.fiap.techchallenge.inventory.api.events.PartPositionMayHaveDroppedEvent;
import com.fiap.techchallenge.inventory.api.events.PurchaseOrderPlacedEvent;
import com.fiap.techchallenge.inventory.api.events.PurchaseOrderReceivedEvent;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.entities.PurchaseOrder;
import com.fiap.techchallenge.inventory.entities.PurchaseOrderLine;
import com.fiap.techchallenge.inventory.entities.Vendor;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.PurchaseOrderNotFoundException;
import com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException;
import com.fiap.techchallenge.inventory.mappers.PurchaseOrderMapper;
import com.fiap.techchallenge.inventory.repositories.PartRepository;
import com.fiap.techchallenge.inventory.repositories.PurchaseOrderRepository;
import com.fiap.techchallenge.inventory.repositories.VendorRepository;
import com.fiap.techchallenge.inventory.repositories.specifications.PurchaseOrderSpecifications;
import com.fiap.techchallenge.inventory.vendor.PartVendorClient;
import com.fiap.techchallenge.inventory.vendor.VendorOrderConfirmation;
import com.fiap.techchallenge.inventory.vendor.VendorOrderLine;
import com.fiap.techchallenge.inventory.vendor.VendorOrderRequest;
import com.fiap.techchallenge.shared.audit.ActorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorRepository vendorRepository;
    private final PartRepository partRepository;
    private final StockLedger ledger;

    private final PartVendorClient vendorClient;
    private final PurchaseOrderMapper mapper;
    private final ApplicationEventPublisher events;
    private final PurchaseOrderStateMachine stateMachine;
    private final ActorResolver actorResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderInfo> listPurchaseOrders(PurchaseOrderFilterQuery filter, Pageable pageable) {
        Specification<PurchaseOrder> spec = Specification
                .where(PurchaseOrderSpecifications.vendorIdEquals(filter.vendorId()))
                .and(PurchaseOrderSpecifications.statusEquals(filter.status()))
                .and(PurchaseOrderSpecifications.codeEquals(filter.code()));

        return purchaseOrderRepository
                .findAll(spec, pageable)
                .map(mapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderInfo getById(UUID id) {
        return purchaseOrderRepository
                .findById(id)
                .map(mapper::toInfo)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(id));
    }

    @Override
    @Transactional
    public PurchaseOrderInfo place(PlacePurchaseOrderCommand command) {
        Vendor vendor = vendorRepository
                .findById(command.vendorId())
                .orElseThrow(() -> new VendorNotFoundException(command.vendorId()));

        PurchaseOrder po = new PurchaseOrder();
        po.setCode(nextPurchaseOrderCode());
        po.setVendor(vendor);
        po.setStatus(PurchaseOrderStatus.PLACED);
        po.setPlacedAt(Instant.now());

        for (PlacePurchaseOrderLineCommand lineCommand : command.lines()) {
            Part part = partRepository
                    .findById(lineCommand.partId())
                    .orElseThrow(() -> new PartNotFoundException(lineCommand.partId()));

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPart(part);
            line.setQuantityOrdered(lineCommand.quantity());
            line.setQuantityReceived(BigDecimal.ZERO);

            po.addLine(line);
        }

        VendorOrderConfirmation confirmation = vendorClient.placeOrder(new VendorOrderRequest(
                vendor.getId(),
                command.lines().stream()
                        .map(l -> new VendorOrderLine(l.partId(), l.quantity()))
                        .toList()
        ));

        po.setVendorOrderRef(confirmation.vendorOrderRef());
        po.setExpectedAt(confirmation.expectedAt());

        purchaseOrderRepository.save(po);

        events.publishEvent(new PurchaseOrderPlacedEvent(
                po.getId(), po.getCode(), vendor.getId(), actorResolver.forCurrentUser(false), mapper.toInfo(po)));

        return mapper.toInfo(po);
    }

    @Override
    @Transactional
    public PurchaseOrderInfo receive(UUID purchaseOrderId, ReceivePurchaseOrderCommand command) {
        PurchaseOrder po = purchaseOrderRepository
                .findById(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));

        Map<UUID, PurchaseOrderLine> linesById = po.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, l -> l));

        for (ReceivePurchaseOrderLineCommand lineCommand : command.lines()) {
            if (!linesById.containsKey(lineCommand.lineId())) {
                throw new IllegalArgumentException(
                        "Line " + lineCommand.lineId() + " does not belong to purchase order " + purchaseOrderId);
            }
        }

        // Resolved and validated against the state machine before anything is mutated: an illegal
        // receipt (the order is already RECEIVED or CANCELLED) must never touch stock at all.
        PurchaseOrderStatus targetStatus = statusAfterReceiving(po, command);
        po.setStatus(stateMachine.transition(po.getStatus(), targetStatus));

        if (targetStatus == PurchaseOrderStatus.RECEIVED) {
            po.setReceivedAt(Instant.now());
        }

        for (ReceivePurchaseOrderLineCommand lineCommand : command.lines()) {
            receiveLine(po, linesById.get(lineCommand.lineId()), lineCommand.quantityReceived(), lineCommand.unitCost());
        }

        events.publishEvent(new PurchaseOrderReceivedEvent(
                po.getId(), po.getCode(), po.getStatus(), actorResolver.forCurrentUser(false), mapper.toInfo(po)));

        // A receipt alone leaves position unchanged (available rises by exactly what inbound falls
        // by), but the shortfall top-up receiveLine() triggers reallocates available stock into
        // reservations, which can lower it — so this still needs re-checking per part touched.
        touchedPartIds(command.lines().stream().map(l -> linesById.get(l.lineId())))
                .forEach(partId -> events.publishEvent(new PartPositionMayHaveDroppedEvent(partId)));

        return mapper.toInfo(po);
    }

    @Override
    @Transactional
    public PurchaseOrderInfo cancel(UUID id) {
        PurchaseOrder po = purchaseOrderRepository
                .findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(id));

        po.setStatus(stateMachine.transition(po.getStatus(), PurchaseOrderStatus.CANCELLED));

        // Cancelling drops inbound with nothing to replace it, which can lower position on its own.
        touchedPartIds(po.getLines().stream())
                .forEach(partId -> events.publishEvent(new PartPositionMayHaveDroppedEvent(partId)));

        return mapper.toInfo(po);
    }

    /**
     * What the purchase order's status would become if every line in {@code command} received what
     * it asks for, without mutating anything yet — so the state machine can validate the transition
     * before {@link #receiveLine} touches any stock.
     */
    private PurchaseOrderStatus statusAfterReceiving(PurchaseOrder po, ReceivePurchaseOrderCommand command) {
        Map<UUID, BigDecimal> incomingByLineId = command.lines().stream()
                .collect(Collectors.toMap(ReceivePurchaseOrderLineCommand::lineId, ReceivePurchaseOrderLineCommand::quantityReceived));

        boolean fullyReceived = po.getLines().stream().allMatch(line -> {
            BigDecimal incoming = incomingByLineId.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal projected = line.getQuantityReceived().add(incoming);

            return projected.compareTo(line.getQuantityOrdered()) == 0;
        });

        return fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    /**
     * One line's receipt: rolls the arrived quantity and cost into the part's on-hand and
     * moving-average cost, writes the PURCHASE movement, and tops up whatever shortfall the part's
     * oldest waiting reservations still carry — this is how a customer's quote ever "catches up" once
     * the shop actually orders the part it was short on.
     */
    private void receiveLine(PurchaseOrder po, PurchaseOrderLine line, BigDecimal quantityReceivedNow, BigDecimal unitCost) {
        Part part = partRepository
                .findByIdForUpdate(line.getPart().getId())
                .orElseThrow(() -> new PartNotFoundException(line.getPart().getId()));

        line.setQuantityReceived(line.getQuantityReceived().add(quantityReceivedNow));
        line.setUnitCost(unitCost);

        ledger.recordPurchase(part, quantityReceivedNow, unitCost, line);
    }

    private String nextPurchaseOrderCode() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = dtf.format(LocalDateTime.now());

        Long nextSeq = purchaseOrderRepository.getNextSequence();

        return "PO-" + formattedDate + "-%06d".formatted(nextSeq);
    }

    private List<UUID> touchedPartIds(Stream<PurchaseOrderLine> lines) {
        return lines
                .map(l -> l.getPart().getId())
                .distinct()
                .toList();
    }
}
