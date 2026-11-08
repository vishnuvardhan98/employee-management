package com.peopledesk.report;

/** Employees who joined in a given calendar month. */
public record MonthCount(Integer year, Integer month, Long count) {

    public String getLabel() {
        return "%04d-%02d".formatted(year, month);
    }
}
