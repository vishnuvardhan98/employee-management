package com.peopledesk.employee;

import java.time.LocalDate;

/** Optional criteria shared by the employee directory and the report. Null values are ignored. */
public record EmployeeFilter(
        String search,
        Long departmentId,
        EmployeeStatus status,
        LocalDate joinedFrom,
        LocalDate joinedTo) {
}
