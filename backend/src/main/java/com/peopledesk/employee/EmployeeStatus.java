package com.peopledesk.employee;

public enum EmployeeStatus {
    ACTIVE("Active"),
    ON_LEAVE("On leave"),
    INACTIVE("Inactive");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
