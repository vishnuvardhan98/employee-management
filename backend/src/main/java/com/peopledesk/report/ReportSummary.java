package com.peopledesk.report;

import java.time.LocalDate;
import java.util.List;

public record ReportSummary(
        LocalDate from,
        LocalDate to,
        long total,
        List<CategoryCount> byStatus,
        List<CategoryCount> byDepartment,
        List<MonthCount> byJoinMonth) {
}
