package com.fiap.techchallenge.scheduling.controllers;

import com.fiap.techchallenge.scheduling.api.SchedulingSettingsService;
import com.fiap.techchallenge.scheduling.api.commands.CreateClosureCommand;
import com.fiap.techchallenge.scheduling.api.commands.UpdateSchedulingSettingsCommand;
import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Shop-wide scheduling settings and closure days. Full endpoint documentation lives on
 * {@link SchedulingSettingsControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class SchedulingSettingsController implements SchedulingSettingsControllerSwaggerDoc {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MANAGER')";

    private final SchedulingSettingsService service;

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<SchedulingSettingsInfo> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SchedulingSettingsInfo> updateSettings(UpdateSchedulingSettingsCommand command) {
        return ResponseEntity.ok(service.updateSettings(command));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<List<ClosureInfo>> listClosures() {
        return ResponseEntity.ok(service.listClosures());
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ClosureInfo> createClosure(CreateClosureCommand command) {
        return ResponseEntity.ok(service.createClosure(command));
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteClosure(LocalDate date) {
        service.deleteClosure(date);
        return ResponseEntity.noContent().build();
    }
}
