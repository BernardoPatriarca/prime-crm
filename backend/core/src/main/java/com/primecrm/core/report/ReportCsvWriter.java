package com.primecrm.core.report;

import com.primecrm.core.dto.report.ReportGroupRow;
import com.primecrm.core.dto.report.ReportResponse;
import com.primecrm.shared.util.CsvWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportCsvWriter {

    private static final String EMPTY_LABEL = "(nao informado)";

    public String write(ReportResponse report) {
        List<String> header = new ArrayList<>(List.of("Grupo", "Quantidade", "Percentual"));
        if (report.measured()) {
            header.add("Total");
        }
        List<List<String>> rows = report.rows().stream().map(row -> cells(row, report.measured())).toList();
        return CsvWriter.write(header, rows);
    }

    private List<String> cells(ReportGroupRow row, boolean measured) {
        List<String> cells = new ArrayList<>(List.of(
                row.label() == null ? EMPTY_LABEL : row.label(),
                String.valueOf(row.count()),
                decimal(row.percentage())));
        if (measured) {
            cells.add(decimal(row.total()));
        }
        return cells;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }
}
