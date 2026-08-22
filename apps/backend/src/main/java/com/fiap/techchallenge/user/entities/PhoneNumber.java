package com.fiap.techchallenge.user.entities;

import com.fiap.techchallenge.user.enums.PhoneType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "phone_numbers",
        schema = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_phone_user", columnNames = {"user_id", "phone"})
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PhoneNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PhoneType type;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
