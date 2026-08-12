package com.primecrm.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.primecrm.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

class DocumentValidatorTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    @Test
    void normalizeAndValidate_maskedCpf_returnsOnlyDigits() {
        assertThat(DocumentValidator.normalizeAndValidate("529.982.247-25")).isEqualTo(VALID_CPF);
    }

    @Test
    void normalizeAndValidate_maskedCnpj_returnsOnlyDigits() {
        assertThat(DocumentValidator.normalizeAndValidate("11.222.333/0001-81")).isEqualTo(VALID_CNPJ);
    }

    @Test
    void normalizeAndValidate_blankDocument_returnsNull() {
        assertThat(DocumentValidator.normalizeAndValidate("   ")).isNull();
        assertThat(DocumentValidator.normalizeAndValidate(null)).isNull();
    }

    @Test
    void normalizeAndValidate_wrongLength_throwsBusinessException() {
        assertThatThrownBy(() -> DocumentValidator.normalizeAndValidate("12345"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void normalizeAndValidate_wrongCheckDigits_throwsBusinessException() {
        assertThatThrownBy(() -> DocumentValidator.normalizeAndValidate("52998224726"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> DocumentValidator.normalizeAndValidate("11222333000182"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void repeatedDigits_areRejected() {
        assertThat(DocumentValidator.isValidCpf("11111111111")).isFalse();
        assertThat(DocumentValidator.isValidCnpj("11111111111111")).isFalse();
    }
}
