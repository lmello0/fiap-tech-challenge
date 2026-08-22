package com.fiap.techchallenge;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(TechChallengeApplication.class);

    /**
     * Guards the one-way dependency that the whole design rests on: auth may reach into user's
     * named interfaces, user may never reach back into auth.
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
