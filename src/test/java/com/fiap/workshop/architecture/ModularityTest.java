package com.fiap.workshop.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fiap.workshop.WorkshopApplication;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

class ModularityTest {

	// the stock saga needs events in both directions: workorder -> inventory (BudgetApproved...) and inventory -> workorder (PartsReserved...)
	private static final Set<Set<String>> TOLERATED_CYCLES = Set.of(Set.of("inventory", "workorder"));

	private static final Pattern CYCLE_HEADER = Pattern.compile("Cycle detected:(.*?)\\R\\s*1\\.", Pattern.DOTALL);
	private static final Pattern SLICE = Pattern.compile("Slice (\\w+)");

	private final ApplicationModules modules = ApplicationModules.of(WorkshopApplication.class);

	@Test
	void modulesRespectTheirBoundaries() {
		modules.detectViolations().filter(violation -> !isToleratedCycle(violation.getMessage())).throwIfPresent();
	}

	@Test
	void everyToleratedCycleStillExists() {
		Violations all = modules.detectViolations();

		Set<Set<String>> found = new HashSet<>();
		all.getMessages().forEach(message -> slicesOf(message).ifPresent(found::add));

		assertThat(found).containsExactlyInAnyOrderElementsOf(TOLERATED_CYCLES);
	}

	@Test
	void onlyTheListedCyclesAreTolerated() {
		assertThat(isToleratedCycle(cycle("inventory", "workorder"))).isTrue();
		assertThat(isToleratedCycle(cycle("workorder", "inventory"))).isTrue();
		assertThat(isToleratedCycle(cycle("customer", "workorder"))).isFalse();
		assertThat(isToleratedCycle(cycle("inventory", "workorder", "catalog"))).isFalse();
		assertThat(isToleratedCycle("Module 'customer' depends on non-exposed type com.fiap.workshop.workorder.domain.WorkOrder")).isFalse();
	}

	private static boolean isToleratedCycle(String message) {
		return slicesOf(message).map(TOLERATED_CYCLES::contains).orElse(false);
	}

	private static java.util.Optional<Set<String>> slicesOf(String message) {
		Matcher header = CYCLE_HEADER.matcher(message);
		if (!header.find()) {
			return java.util.Optional.empty();
		}
		Set<String> slices = new HashSet<>();
		Matcher slice = SLICE.matcher(header.group(1));
		while (slice.find()) {
			slices.add(slice.group(1));
		}
		return java.util.Optional.of(slices);
	}

	private static String cycle(String... slices) {
		StringBuilder header = new StringBuilder("Cycle detected: ");
		for (String slice : slices) {
			header.append("Slice ").append(slice).append(" -> \n                ");
		}
		header.append("Slice ").append(slices[0]);
		return header + "\n  1. Dependencies of Slice " + slices[0] + "\n    - Method <x> has parameter of type <y>";
	}
}
