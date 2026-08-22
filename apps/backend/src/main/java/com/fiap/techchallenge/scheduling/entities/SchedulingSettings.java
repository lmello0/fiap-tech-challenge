package com.fiap.techchallenge.scheduling.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

/**
 * The Operating Calendar's Manager-editable settings (CONTEXT.md): business hours and per-type Slot
 * Capacity. A singleton row — the weekly Monday-Friday workdays themselves are a fixed constant in
 * code (see {@code AvailabilityService}), not configurable.
 */
@Entity
@Table(name = "scheduling_settings", schema = "scheduling")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SchedulingSettings {

    @Id
    private UUID id;

    @Column(nullable = false)
    private LocalTime businessStartTime;

    @Column(nullable = false)
    private LocalTime businessEndTime;

    @Column(nullable = false)
    private int dropoffSlotCapacity;

    @Column(nullable = false)
    private int pickupSlotCapacity;

    public int capacityFor(com.fiap.techchallenge.scheduling.enums.AppointmentType type) {
        return type == com.fiap.techchallenge.scheduling.enums.AppointmentType.DROPOFF
                ? dropoffSlotCapacity
                : pickupSlotCapacity;
    }
}
