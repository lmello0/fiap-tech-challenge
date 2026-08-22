package com.fiap.techchallenge.scheduling.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A specific future date a Manager has marked as not open, overriding the Operating Calendar's
 * default weekly hours for that date (CONTEXT.md "Closure").
 */
@Entity
@Table(name = "closures", schema = "scheduling")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Closure {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private LocalDate date;

    @Column(length = 500)
    private String message;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
