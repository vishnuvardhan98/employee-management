package com.peopledesk.report;

import com.peopledesk.employee.EmployeeResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Builds the .xlsx export with Apache POI. */
@Component
public class ExcelReportWriter {

    static final String[] HEADERS = {"ID", "Name", "Email", "Phone", "Department", "Job title", "Status", "Join date"};
    private static final int[] WIDTHS = {8, 26, 36, 18, 18, 28, 14, 14};

    public byte[] write(List<EmployeeResponse> rows, String filterDescription, String generatedAt) {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Employees");
            CellStyle header = headerStyle(book);
            CellStyle date = book.createCellStyle();
            date.setDataFormat(book.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(header);
                sheet.setColumnWidth(i, WIDTHS[i] * 256);
            }

            int r = 1;
            for (EmployeeResponse e : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.id());
                row.createCell(1).setCellValue(e.name());
                row.createCell(2).setCellValue(e.email());
                row.createCell(3).setCellValue(e.phone());
                row.createCell(4).setCellValue(e.departmentName());
                row.createCell(5).setCellValue(e.title());
                row.createCell(6).setCellValue(e.status().label());
                Cell joined = row.createCell(7);
                joined.setCellValue(Date.valueOf(e.joinedOn()));
                joined.setCellStyle(date);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, r - 1), 0, HEADERS.length - 1));

            Sheet info = book.createSheet("Report info");
            info.setColumnWidth(0, 16 * 256);
            info.setColumnWidth(1, 80 * 256);
            String[][] meta = {
                    {"Report", "PeopleDesk employee report"},
                    {"Filters", filterDescription},
                    {"Generated", generatedAt},
                    {"Records", String.valueOf(rows.size())}};
            for (int i = 0; i < meta.length; i++) {
                Row row = info.createRow(i);
                Cell label = row.createCell(0);
                label.setCellValue(meta[i][0]);
                label.setCellStyle(header);
                row.createCell(1).setCellValue(meta[i][1]);
            }

            book.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not build the Excel report", ex);
        }
    }

    private static CellStyle headerStyle(Workbook book) {
        Font font = book.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
