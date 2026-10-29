package com.fiap.workshop.architecture;

import com.fiap.workshop.WorkshopApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build when a module reaches into another module's internals or modules form a cycle. */
class ModularityTest {

	@Test
	void modulesRespectTheirBoundaries() {
		ApplicationModules.of(WorkshopApplication.class).verify();
	}
}
