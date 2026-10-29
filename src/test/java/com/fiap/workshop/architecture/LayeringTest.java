package com.fiap.workshop.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.fiap.workshop", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

	@ArchTest
	static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "org.hibernate..", "lombok..", "com.fasterxml..", "tools.jackson..")
			.because("the domain layer is plain Java");

	@ArchTest
	static final ArchRule applicationDoesNotDependOnAdapters = noClasses().that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..adapter..")
			.because("dependencies point inwards: adapters depend on the application, never the reverse")
			.allowEmptyShould(true); // no module has an application layer yet

	@ArchTest
	static final ArchRule domainDoesNotDependOnOuterLayers = noClasses().that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
			.allowEmptyShould(true);
}
