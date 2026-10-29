package com.fiap.workshop.architecture;

import com.fiap.workshop.WorkshopApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

	@Test
	void modulesRespectTheirBoundaries() {
		ApplicationModules.of(WorkshopApplication.class).verify();
	}
}
