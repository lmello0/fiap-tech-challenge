package com.fiap.techchallenge.vehicle.controllers;

import com.fiap.techchallenge.shared.responses.PageResponse;
import com.fiap.techchallenge.user.enums.WorkerRole;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.commands.UpdateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.queries.VehicleFilterQuery;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.vehicle.exceptions.VehicleNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Full endpoint documentation lives on {@link VehicleControllerSwaggerDoc}. */
@RestController
@RequiredArgsConstructor
public class VehicleController implements VehicleControllerSwaggerDoc {

    private final VehicleService vehicleService;

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ATTENDANT', 'MANAGER')")
    public ResponseEntity<PageResponse<VehicleInfo>> getAllVehicles(
            VehicleFilterQuery filter,
            Pageable pageable,
            Authentication authentication
    ) {
        VehicleFilterQuery effectiveFilter = isStaff(authentication)
                ? filter
                : filter.withCustomerId(UUID.fromString(authentication.getName()));

        return ResponseEntity.ok(PageResponse.from(vehicleService.listVehicles(effectiveFilter, pageable)));
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ATTENDANT', 'MANAGER')")
    public ResponseEntity<VehicleInfo> getById(UUID id, Authentication authentication) {
        VehicleInfo vehicle = vehicleService.getById(id);
        requireOwnershipOrStaff(vehicle, authentication);

        return ResponseEntity.ok(vehicle);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ATTENDANT', 'MANAGER')")
    public ResponseEntity<VehicleInfo> create(CreateVehicleCommand command, Authentication authentication) {
        CreateVehicleCommand effectiveCommand = isStaff(authentication)
                ? requireCustomerId(command)
                : command.withCustomerId(UUID.fromString(authentication.getName()));

        VehicleInfo vehicle = vehicleService.create(effectiveCommand);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(vehicle.id())
                .toUri();

        return ResponseEntity.created(location).body(vehicle);
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ATTENDANT', 'MANAGER')")
    public ResponseEntity<VehicleInfo> update(UUID id, UpdateVehicleCommand command, Authentication authentication) {
        requireOwnershipOrStaff(vehicleService.getById(id), authentication);

        return ResponseEntity.ok(vehicleService.update(id, command));
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ATTENDANT', 'MANAGER')")
    public ResponseEntity<Void> deactivate(UUID id, Authentication authentication) {
        requireOwnershipOrStaff(vehicleService.getById(id), authentication);
        vehicleService.deactivate(id);

        return ResponseEntity.noContent().build();
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + WorkerRole.ATTENDANT)
                        || authority.equals("ROLE_" + WorkerRole.MANAGER));
    }

    /** A mismatch throws not-found rather than access-denied, so as not to reveal that another
     * customer's vehicle exists — same masking as the workorder and appointment modules. */
    private void requireOwnershipOrStaff(VehicleInfo vehicle, Authentication authentication) {
        if (isStaff(authentication)) {
            return;
        }

        if (!vehicle.customerId().toString().equals(authentication.getName())) {
            throw new VehicleNotFoundException(vehicle.id());
        }
    }

    private CreateVehicleCommand requireCustomerId(CreateVehicleCommand command) {
        if (command.customerId() == null) {
            throw new IllegalArgumentException("customerId is required");
        }

        return command;
    }
}
