package com.peopledesk.common;

/** Thrown when a value that must be unique (e.g. an employee email) already exists. */
public class DuplicateFieldException extends RuntimeException {

    private final String field;

    public DuplicateFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
