package com.peopledesk.report;

import com.peopledesk.common.PageResponse;
import com.peopledesk.employee.EmployeeResponse;
import com.peopledesk.employee.EmployeeSort;
import com.peopledesk.employee.EmployeeStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only report endpoints (ROLE_ADMIN only, see SecurityConfig). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final ReportService reports;
    private final ExcelReportWriter excel;
    private final PdfReportWriter pdf;
    private final Clock clock;

    public ReportController(ReportService reports, ExcelReportWriter excel, PdfReportWriter pdf, Clock clock) {
        this.reports = reports;
        this.excel = excel;
        this.pdf = pdf;
        this.clock = clock;
    }

    @GetMapping("/employees")
    public PageResponse<EmployeeResponse> employees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joined") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        ReportFilter filter = reports.filter(from, to, departmentId, status);
        return reports.rows(filter, EmployeeSort.pageable(page, size, sort, direction));
    }

    @GetMapping("/summary")
    public ReportSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status) {
        return reports.summary(reports.filter(from, to, departmentId, status));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "joined") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        ReportFilter filter = reports.filter(from, to, departmentId, status);
        List<EmployeeResponse> rows = reports.exportRows(filter, EmployeeSort.of(sort, direction));
        byte[] body = excel.write(rows, reports.describe(filter), now());
        return download(body, XLSX, fileName(filter, "xlsx"));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(defaultValue = "joined") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        ReportFilter filter = reports.filter(from, to, departmentId, status);
        List<EmployeeResponse> rows = reports.exportRows(filter, EmployeeSort.of(sort, direction));
        byte[] body = pdf.write(rows, reports.describe(filter), now());
        return download(body, MediaType.APPLICATION_PDF, fileName(filter, "pdf"));
    }

    private String now() {
        return ZonedDateTime.now(clock).format(GENERATED);
    }

    private static String fileName(ReportFilter filter, String extension) {
        return "employee-report_%s_to_%s.%s".formatted(filter.from(), filter.to(), extension);
    }

    private static ResponseEntity<byte[]> download(byte[] body, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
