package com.primecrm.core.validation;

import com.primecrm.shared.exception.BusinessException;
import org.springframework.util.StringUtils;

public final class DocumentValidator {

    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;
    private static final int[] CNPJ_FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] CNPJ_SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private DocumentValidator() {
    }

    public static String normalize(String rawDocument) {
        if (!StringUtils.hasText(rawDocument)) {
            return null;
        }
        String digits = rawDocument.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String normalizeAndValidate(String rawDocument) {
        String digits = normalize(rawDocument);
        if (digits == null) {
            return null;
        }
        if (digits.length() == CPF_LENGTH) {
            requireValid(isValidCpf(digits), "CPF informado e invalido");
            return digits;
        }
        if (digits.length() == CNPJ_LENGTH) {
            requireValid(isValidCnpj(digits), "CNPJ informado e invalido");
            return digits;
        }
        throw new BusinessException("INVALID_DOCUMENT",
                "Documento deve conter 11 digitos (CPF) ou 14 digitos (CNPJ)");
    }

    public static boolean isValidCpf(String digits) {
        if (digits == null || digits.length() != CPF_LENGTH || allSameDigit(digits)) {
            return false;
        }
        int first = cpfCheckDigit(digits, 9, 10);
        int second = cpfCheckDigit(digits, 10, 11);
        return first == digitAt(digits, 9) && second == digitAt(digits, 10);
    }

    public static boolean isValidCnpj(String digits) {
        if (digits == null || digits.length() != CNPJ_LENGTH || allSameDigit(digits)) {
            return false;
        }
        int first = weightedCheckDigit(digits, CNPJ_FIRST_WEIGHTS);
        int second = weightedCheckDigit(digits, CNPJ_SECOND_WEIGHTS);
        return first == digitAt(digits, 12) && second == digitAt(digits, 13);
    }

    private static void requireValid(boolean valid, String message) {
        if (!valid) {
            throw new BusinessException("INVALID_DOCUMENT", message);
        }
    }

    private static int cpfCheckDigit(String digits, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digitAt(digits, i) * (startWeight - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int weightedCheckDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digitAt(digits, i) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean allSameDigit(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    private static int digitAt(String digits, int index) {
        return Character.getNumericValue(digits.charAt(index));
    }
}
