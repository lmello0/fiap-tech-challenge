package com.fiap.techchallenge.user.api.representation;

import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.WorkerRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserInfo(
        UUID id,
        String firstName,
        String lastName,
        String email,
        DocumentType documentType,
        String documentCode,
        List<PhoneNumberInfo> phoneNumbers,
        boolean emailVerified,
        boolean customer,
        boolean customerActive,
        boolean worker,
        WorkerRole workerRole,
        String registration,
        LocalDate hireDate,
        LocalDate terminationDate,
        Instant createdAt,
        Instant updatedAt
) {
}
