package com.fiap.techchallenge.user.services;

import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.exceptions.InvalidDocumentException;

public class DocumentValidator {

    public static boolean validate(DocumentType type, String code) {
        return switch (type) {
            case CPF -> isValidCpf(code);
            case CNPJ -> isValidCnpj(code);
        };
    }

    private static boolean isValidCpf(String code) {
        if (code == null) {
            return false;
        }

        String digits = code.replaceAll("\\D", "");

        if (digits.length() != 11) {
            return false;
        }

        if (digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateCpfDigit(digits.substring(0, 9));
        int secondDigit = calculateCpfDigit(digits.substring(0, 9) + firstDigit);

        return digits.equals(
                digits.substring(0, 9) + firstDigit + secondDigit
        );
    }

    private static int calculateCpfDigit(String value) {
        int length = value.length();

        int sum = 0;

        for (int i = 0; i < length; i++) {
            int digit = value.charAt(i) - '0';
            int weight = length + 1 - i;

            sum += digit * weight;
        }

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean isValidCnpj(String code) {
        if (code == null) {
            return false;
        }

        String value = code.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (value.length() != 14) {
            return false;
        }
        if (!value.substring(0, 12).matches("[A-Z0-9]{12}")) {
            return false;
        }

        if (!value.substring(12).matches("\\d{2}")) {
            return false;
        }

        int firstDigit = calculateCnpjDigit(value.substring(0, 12));
        int secondDigit = calculateCnpjDigit(value.substring(0, 12) + firstDigit);

        return value.charAt(12) - '0' == firstDigit
                && value.charAt(13) - '0' == secondDigit;
    }

    private static int calculateCnpjDigit(String value) {
        int sum = 0;
        int weight = 2;

        for (int i = value.length() - 1; i >= 0; i--) {
            int characterValue = cnpjCharacterValue(value.charAt(i));

            sum += characterValue * weight;

            weight++;

            if (weight > 9) {
                weight = 2;
            }
        }

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int cnpjCharacterValue(char character) {
        if (character >= '0' && character <= '9') {
            return character - '0';
        }

        if (character >= 'A' && character <= 'Z') {
            return character - 48;
        }

        throw new InvalidDocumentException(
                "Invalid CNPJ character: " + character
        );
    }
}
