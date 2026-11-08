package com.peopledesk.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.peopledesk.employee.EmployeeResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/** Builds the PDF export with OpenPDF (an LGPL fork of iText): title, filters, table, header/footer. */
@Component
public class PdfReportWriter {

    private static final Color BRAND = new Color(25, 79, 70);
    private static final Color STRIPE = new Color(244, 247, 241);
    private static final String[] HEADERS = {"Name", "Email", "Phone", "Department", "Job title", "Status", "Join date"};
    private static final float[] WIDTHS = {16, 24, 12, 12, 17, 9, 10};

    public byte[] write(List<EmployeeResponse> rows, String filterDescription, String generatedAt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 42);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooter(generatedAt));
            document.addTitle("PeopleDesk employee report");
            document.open();

            Paragraph title = new Paragraph("Employee report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND));
            title.setSpacingAfter(4);
            document.add(title);
            Font meta = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            document.add(new Paragraph(filterDescription, meta));
            Paragraph count = new Paragraph("Generated " + generatedAt + "  |  " + rows.size() + " employees", meta);
            count.setSpacingAfter(12);
            document.add(count);

            document.add(table(rows));
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not build the PDF report", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private static PdfPTable table(List<EmployeeResponse> rows) {
        PdfPTable table = new PdfPTable(WIDTHS);
        table.setWidthPercentage(100);
        table.setHeaderRows(1); // repeat the header row on every page

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(6);
            cell.setBorderColor(BRAND);
            table.addCell(cell);
        }

        Font body = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);
        boolean stripe = false;
        for (EmployeeResponse e : rows) {
            for (String value : new String[]{e.name(), e.email(), e.phone(), e.departmentName(), e.title(),
                    e.status().label(), e.joinedOn().toString()}) {
                PdfPCell cell = new PdfPCell(new Phrase(value, body));
                cell.setPadding(5);
                cell.setBorderColor(new Color(225, 230, 222));
                if (stripe) {
                    cell.setBackgroundColor(STRIPE);
                }
                table.addCell(cell);
            }
            stripe = !stripe;
        }
        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No employees match these filters.", body));
            empty.setColspan(HEADERS.length);
            empty.setPadding(10);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(empty);
        }
        return table;
    }

    /** Company header and page-numbered footer drawn on every page. */
    private static final class HeaderFooter extends PdfPageEventHelper {

        private final Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        private final Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND);
        private final String generatedAt;

        HeaderFooter(String generatedAt) {
            this.generatedAt = generatedAt;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("PeopleDesk  ·  Employee Management", brand),
                    document.left(), page.getHeight() - 30, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Confidential — internal use only  |  " + generatedAt, font),
                    document.left(), 22, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), font),
                    document.right(), 22, 0);
        }
    }
}
