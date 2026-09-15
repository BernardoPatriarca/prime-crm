package com.primecrm.core.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentPdfWriterTest {

    private final DocumentPdfWriter writer = new DocumentPdfWriter();

    @Test
    void write_producesAValidPdfWithItemsAndTotal() {
        DocumentPdfModel model = new DocumentPdfModel(
                "Proposta Comercial",
                "PRO-001000",
                List.of(new DocumentPdfModel.DocumentPdfField("Cliente", "Acme Ltda")),
                List.of(new DocumentPdfModel.DocumentPdfItem("Consultoria", BigDecimal.ONE,
                        new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"))),
                new BigDecimal("100.00"),
                "Observacao de teste");

        byte[] content = writer.write(model);

        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void write_withoutItemsOrNotes_stillProducesAValidPdf() {
        DocumentPdfModel model = new DocumentPdfModel(
                "Contrato",
                "CTR-001000",
                List.of(new DocumentPdfModel.DocumentPdfField("Cliente", null)),
                List.of(),
                new BigDecimal("500.00"),
                null);

        byte[] content = writer.write(model);

        assertThat(content).isNotEmpty();
        assertThat(new String(content, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
