package com.fiap.workshop.customer.api;

import java.util.Optional;
import java.util.UUID;

public interface CustomerQueries {

	Optional<CustomerView> findCustomerById(UUID id);

	Optional<CustomerView> findCustomerByDocument(String document);

	Optional<VehicleView> findVehicleById(UUID id);

	Optional<VehicleView> findVehicleByPlate(String plate);
}
