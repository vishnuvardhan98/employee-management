package com.peopledesk.report;

import com.peopledesk.employee.EmployeeFilter;
import com.peopledesk.employee.EmployeeStatus;
import java.time.LocalDate;

/** Validated report criteria; from/to are always set (defaults to the current month). */
public record ReportFilter(LocalDate from, LocalDate to, Long departmentId, EmployeeStatus status) {

    public EmployeeFilter toEmployeeFilter() {
        return new EmployeeFilter(null, departmentId, status, from, to);
    }
}
