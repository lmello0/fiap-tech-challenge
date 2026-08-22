package com.fiap.techchallenge.user.entities;

import com.fiap.techchallenge.user.enums.WorkerRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workers", schema = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Worker {

    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String registration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WorkerRole role;

    @Column(nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate terminationDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void terminate(LocalDate on) {
        this.terminationDate = on;
        this.isActive = false;
    }
}
