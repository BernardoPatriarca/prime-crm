package com.primecrm.shared.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CsvWriter {

    public static final String SEPARATOR = ";";

    private static final String LINE_BREAK = "\r\n";
    private static final String BYTE_ORDER_MARK = "﻿";

    private CsvWriter() {
    }

    public static String write(List<String> header, List<List<String>> rows) {
        String content = Stream.concat(Stream.of(header), rows.stream())
                .map(CsvWriter::line)
                .collect(Collectors.joining(LINE_BREAK));
        return BYTE_ORDER_MARK + content + LINE_BREAK;
    }

    private static String line(List<String> cells) {
        return cells.stream().map(CsvWriter::escape).collect(Collectors.joining(SEPARATOR));
    }

    private static String escape(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
