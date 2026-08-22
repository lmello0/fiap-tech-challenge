package com.fiap.techchallenge.scheduling.api.representation;

import java.time.LocalTime;

public record SchedulingSettingsInfo(
        LocalTime businessStartTime,
        LocalTime businessEndTime,
        int dropoffSlotCapacity,
        int pickupSlotCapacity
) {
}
