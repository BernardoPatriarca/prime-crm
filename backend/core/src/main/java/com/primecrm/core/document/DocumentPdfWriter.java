package com.primecrm.core.document;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

@Component
public class DocumentPdfWriter {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font CODE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.ITALIC);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final DecimalFormat CURRENCY_FORMAT =
            new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));

    public byte[] write(DocumentPdfModel model) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(new Paragraph(model.documentTitle(), TITLE_FONT));
            document.add(new Paragraph(model.code(), CODE_FONT));
            document.add(Chunk.NEWLINE);

            document.add(headerTable(model.headerFields()));
            document.add(Chunk.NEWLINE);

            if (!model.items().isEmpty()) {
                document.add(new Paragraph("Itens", SECTION_FONT));
                document.add(itemsTable(model.items()));
                document.add(Chunk.NEWLINE);
            }

            Paragraph total = new Paragraph("Total: " + CURRENCY_FORMAT.format(model.totalAmount()), TOTAL_FONT);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            if (model.notes() != null && !model.notes().isBlank()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Observacoes", SECTION_FONT));
                document.add(new Paragraph(model.notes(), VALUE_FONT));
            }

            document.close();
            return output.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o documento PDF", e);
        }
    }

    private PdfPTable headerTable(List<DocumentPdfModel.DocumentPdfField> fields) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        for (DocumentPdfModel.DocumentPdfField field : fields) {
            table.addCell(borderlessCell(field.label(), LABEL_FONT));
            table.addCell(borderlessCell(field.value() == null || field.value().isBlank() ? "-" : field.value(),
                    VALUE_FONT));
        }
        return table;
    }

    private PdfPTable itemsTable(List<DocumentPdfModel.DocumentPdfItem> items) {
        PdfPTable table = new PdfPTable(new float[]{4, 1, 1.3f, 1, 1.3f});
        table.setWidthPercentage(100);
        table.addCell(headerCell("Descricao"));
        table.addCell(headerCell("Qtd"));
        table.addCell(headerCell("Preco unit."));
        table.addCell(headerCell("Desc. %"));
        table.addCell(headerCell("Total"));

        for (DocumentPdfModel.DocumentPdfItem item : items) {
            table.addCell(bodyCell(item.description(), Element.ALIGN_LEFT));
            table.addCell(bodyCell(item.quantity().stripTrailingZeros().toPlainString(), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(CURRENCY_FORMAT.format(item.unitPrice()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(percent(item.discountPercent()), Element.ALIGN_RIGHT));
            table.addCell(bodyCell(CURRENCY_FORMAT.format(item.total()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private String percent(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString() + "%";
    }

    private PdfPCell borderlessCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_CELL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }
}
