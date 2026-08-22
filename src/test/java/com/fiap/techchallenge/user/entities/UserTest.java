package com.fiap.techchallenge.user.entities;

import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void removePhoneNumberTakesItOutOfTheListAndClearsTheBackReference() {
        User user = newUser();
        PhoneNumber phone = PhoneNumber.builder().type(PhoneType.MOBILE).phone("11999999999").isPrimary(true).build();

        user.addPhoneNumber(phone);
        assertThat(user.getPhoneNumbers()).containsExactly(phone);
        assertThat(phone.getUser()).isSameAs(user);

        user.removePhoneNumber(phone);

        assertThat(user.getPhoneNumbers()).isEmpty();
        assertThat(phone.getUser()).isNull();
    }

    @Test
    void becomeCustomerIsANoOpWhenAlreadyACustomer() {
        User user = newUser();
        user.becomeCustomer();
        Customer firstCustomerFacet = user.getCustomer();

        user.becomeCustomer();

        assertThat(user.getCustomer()).isSameAs(firstCustomerFacet);
    }

    @Test
    void isCustomerIsFalseBeforeBecomingOne() {
        User user = newUser();

        assertThat(user.isCustomer()).isFalse();

        user.becomeCustomer();

        assertThat(user.isCustomer()).isTrue();
    }

    @Test
    void isWorkerReflectsWhetherAWorkerFacetIsPresent() {
        User user = newUser();

        assertThat(user.isWorker()).isFalse();

        user.becomeWorker(Worker.builder().registration("ARS-000001").isActive(true).build());

        assertThat(user.isWorker()).isTrue();
    }

    private User newUser() {
        return User.builder()
                .firstName("Ana")
                .lastName("Souza")
                .email("ana@example.com")
                .documentType(DocumentType.CPF)
                .documentCode("52998224725")
                .build();
    }
}
