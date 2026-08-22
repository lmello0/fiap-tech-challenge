package com.fiap.techchallenge.scheduling.api;

import com.fiap.techchallenge.scheduling.api.commands.CreateClosureCommand;
import com.fiap.techchallenge.scheduling.api.commands.UpdateSchedulingSettingsCommand;
import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;

import java.time.LocalDate;
import java.util.List;

public interface SchedulingSettingsService {

    SchedulingSettingsInfo getSettings();

    SchedulingSettingsInfo updateSettings(UpdateSchedulingSettingsCommand command);

    List<ClosureInfo> listClosures();

    /**
     * Creating a Closure on a date with existing SCHEDULED appointments auto-cancels them (reason
     * MANAGER_CLOSURE, carrying {@code command.message()}) and notifies each affected Customer/Guest.
     */
    ClosureInfo createClosure(CreateClosureCommand command);

    void deleteClosure(LocalDate date);
}
