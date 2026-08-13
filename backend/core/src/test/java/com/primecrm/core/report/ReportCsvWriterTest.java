package com.primecrm.core.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.core.dto.report.ReportGroupRow;
import com.primecrm.core.dto.report.ReportResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportCsvWriterTest {

    private final ReportCsvWriter csvWriter = new ReportCsvWriter();

    @Test
    void write_withoutMeasure_omitsTheTotalColumn() {
        ReportResponse report = new ReportResponse("TASKS", "STATUS", false, 2, null, Instant.now(),
                List.of(new ReportGroupRow("PENDING", 2, null, new BigDecimal("100.00"))));

        String csv = csvWriter.write(report);

        assertThat(csv).contains("\"Grupo\";\"Quantidade\";\"Percentual\"").doesNotContain("\"Total\"");
        assertThat(csv).contains("\"PENDING\";\"2\";\"100,00\"");
    }

    @Test
    void write_withMeasure_includesTheTotalColumnUsingCommaAsDecimalSeparator() {
        ReportResponse report = new ReportResponse("OPPORTUNITIES", "STAGE", true, 1, new BigDecimal("1500.50"),
                Instant.now(),
                List.of(new ReportGroupRow("Proposta", 1, new BigDecimal("1500.50"), new BigDecimal("100.00"))));

        String csv = csvWriter.write(report);

        assertThat(csv).contains("\"Total\"");
        assertThat(csv).contains("\"Proposta\";\"1\";\"100,00\";\"1500,50\"");
    }

    @Test
    void write_withNullLabel_fallsBackToAPlaceholder() {
        ReportResponse report = new ReportResponse("CUSTOMERS", "SEGMENT", false, 1, null, Instant.now(),
                List.of(new ReportGroupRow(null, 1, null, new BigDecimal("100.00"))));

        assertThat(csvWriter.write(report)).contains("\"(nao informado)\"");
    }
}
