package com.fiap.techchallenge.shared.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            LocalDate start = getDate(value, startField);
            LocalDate end = getDate(value, endField);

            if (start == null || end == null) {
                return true;
            }

            return !start.isAfter(end);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Could not validate date range: fields '%s' and '%s'".formatted(startField, endField), e);
        }
    }

    private LocalDate getDate(Object object, String fieldName) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        Object value = field.get(object);

        if (value == null) {
            return null;
        }

        if (!(value instanceof LocalDate date)) {
            throw new IllegalArgumentException("Field '%s' must be a LocalDate".formatted(fieldName));
        }

        return date;
    }
}
