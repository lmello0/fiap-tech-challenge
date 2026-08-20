package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateVendorCommand;
import com.fiap.techchallenge.inventory.api.queries.VendorFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VendorService {

    Page<VendorInfo> listVendors(VendorFilterQuery filter, Pageable pageable);

    VendorInfo getById(UUID id);

    VendorInfo create(CreateVendorCommand command);

    VendorInfo update(UUID id, UpdateVendorCommand command);

    void deactivate(UUID id);
}
