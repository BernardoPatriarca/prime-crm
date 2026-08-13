package com.primecrm.core.audit;

import com.primecrm.core.dto.audit.AuditLogResponse;
import com.primecrm.shared.util.CsvWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AuditLogCsvWriter {

    private static final List<String> HEADER =
            List.of("Data", "Acao", "Entidade", "Registro", "Usuario", "IP", "Alteracoes");

    public String write(List<AuditLogResponse> entries) {
        return CsvWriter.write(HEADER, entries.stream().map(this::cells).toList());
    }

    private List<String> cells(AuditLogResponse entry) {
        return List.of(
                String.valueOf(entry.createdAt()),
                entry.action().name(),
                text(entry.entityName()),
                text(entry.entityId()),
                text(entry.userEmail()),
                text(entry.ipAddress()),
                changes(entry.changes()));
    }

    private String changes(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) {
            return "";
        }
        return changes.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" | "));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
