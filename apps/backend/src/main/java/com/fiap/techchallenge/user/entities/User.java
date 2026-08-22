package com.fiap.techchallenge.user.entities;

import com.fiap.techchallenge.user.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        schema = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_document", columnNames = {"document_type", "document_code"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String firstName;

    @Column(length = 30)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType documentType;

    @Column(nullable = false, length = 50)
    private String documentCode;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PhoneNumber> phoneNumbers = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Customer customer;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Worker worker;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addPhoneNumber(PhoneNumber phone) {
        phoneNumbers.add(phone);
        phone.setUser(this);
    }

    public void removePhoneNumber(PhoneNumber phone) {
        phoneNumbers.remove(phone);
        phone.setUser(null);
    }

    public void becomeCustomer() {
        if (customer == null) {
            customer = new Customer(this);
        }
    }

    public void becomeWorker(Worker newWorker) {
        newWorker.setUser(this);
        this.worker = newWorker;
    }

    public boolean isCustomer() {
        return customer != null;
    }

    public boolean isWorker() {
        return worker != null;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void changeEmail(String newEmail) {
        this.email = newEmail;
        this.emailVerified = true;
    }
}
