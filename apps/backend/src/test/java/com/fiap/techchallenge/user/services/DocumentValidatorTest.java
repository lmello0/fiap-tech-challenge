package com.fiap.techchallenge.user.services;

import com.fiap.techchallenge.user.enums.DocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CPF and CNPJ are both check-digit-validated formats, so valid/invalid examples here are fixed,
 * pre-verified values rather than randomly generated ones (a random string almost never has valid
 * check digits, and computing one on the fly per-test would just duplicate this validator's own
 * logic).
 */
class DocumentValidatorTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11444777000161";

    // --- CPF ------------------------------------------------------------------------------------

    @Test
    void aValidCpfPasses() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, VALID_CPF)).isTrue();
    }

    @Test
    void aValidCpfWithPunctuationPasses() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, "529.982.247-25")).isTrue();
    }

    @Test
    void aNullCpfFails() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, null)).isFalse();
    }

    @Test
    void aCpfWithTheWrongLengthFails() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, "1234567890")).isFalse();
        assertThat(DocumentValidator.validate(DocumentType.CPF, "123456789012")).isFalse();
    }

    @Test
    void aCpfWithAllSameDigitsFails() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, "11111111111")).isFalse();
    }

    @Test
    void aCpfWithWrongCheckDigitsFails() {
        assertThat(DocumentValidator.validate(DocumentType.CPF, "52998224700")).isFalse();
    }

    // --- CNPJ -----------------------------------------------------------------------------------

    @Test
    void aValidCnpjPasses() {
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, VALID_CNPJ)).isTrue();
    }

    @Test
    void aValidCnpjWithPunctuationPasses() {
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "11.444.777/0001-61")).isTrue();
    }

    @Test
    void aNullCnpjFails() {
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, null)).isFalse();
    }

    @Test
    void aCnpjWithTheWrongLengthFails() {
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "1144477700016")).isFalse();
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "114447770001611")).isFalse();
    }

    @Test
    void aCnpjWithWrongCheckDigitsFails() {
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "11444777000160")).isFalse();
    }

    @Test
    void aCnpjWhoseLastTwoCharactersArentDigitsFails() {
        // the check-digit positions must be numeric even though the first 12 may be alphanumeric
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "1144477700AB61")).isFalse();
    }

    @Test
    void anAlphanumericCnpjWithAValidCheckDigitPasses() {
        // Receita Federal's alphanumeric CNPJ format: letters allowed in the first 12 positions,
        // each contributing (charCode - 48) per calculateCnpjDigit/cnpjCharacterValue. Check digits
        // computed independently against that exact formula, not guessed.
        assertThat(DocumentValidator.validate(DocumentType.CNPJ, "12ABC345010060")).isTrue();
    }
}
