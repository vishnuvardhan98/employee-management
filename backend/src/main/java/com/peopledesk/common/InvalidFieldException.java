package com.peopledesk.common;

/** A request field passed bean validation but refers to something that does not exist (400). */
public class InvalidFieldException extends RuntimeException {

    private final String field;

    public InvalidFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
