package com.fiap.techchallenge.shared.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default "{start} must be before or equal to {end}";

    String start();

    String end();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
