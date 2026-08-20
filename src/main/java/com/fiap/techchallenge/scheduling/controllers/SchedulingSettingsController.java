package com.fiap.techchallenge.scheduling.controllers;

import com.fiap.techchallenge.scheduling.api.SchedulingSettingsService;
import com.fiap.techchallenge.scheduling.api.commands.CreateClosureCommand;
import com.fiap.techchallenge.scheduling.api.commands.UpdateSchedulingSettingsCommand;
import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("scheduling")
@RequiredArgsConstructor
public class SchedulingSettingsController {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MANAGER')";

    private final SchedulingSettingsService service;

    @GetMapping("/settings")
    @PreAuthorize(STAFF)
    public ResponseEntity<SchedulingSettingsInfo> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SchedulingSettingsInfo> updateSettings(
            @Valid @RequestBody UpdateSchedulingSettingsCommand command
    ) {
        return ResponseEntity.ok(service.updateSettings(command));
    }

    @GetMapping("/closures")
    @PreAuthorize(STAFF)
    public ResponseEntity<List<ClosureInfo>> listClosures() {
        return ResponseEntity.ok(service.listClosures());
    }

    @PostMapping("/closures")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ClosureInfo> createClosure(@Valid @RequestBody CreateClosureCommand command) {
        return ResponseEntity.ok(service.createClosure(command));
    }

    @DeleteMapping("/closures/{date}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteClosure(@PathVariable LocalDate date) {
        service.deleteClosure(date);
        return ResponseEntity.noContent().build();
    }
}
