package com.primecrm.core.document;

import java.math.BigDecimal;
import java.util.List;

public record DocumentPdfModel(
        String documentTitle,
        String code,
        List<DocumentPdfField> headerFields,
        List<DocumentPdfItem> items,
        BigDecimal totalAmount,
        String notes
) {

    public record DocumentPdfField(String label, String value) {
    }

    public record DocumentPdfItem(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal total
    ) {
    }
}
