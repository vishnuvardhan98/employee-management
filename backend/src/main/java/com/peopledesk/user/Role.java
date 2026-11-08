package com.peopledesk.user;

/**
 * ADMIN: full employee CRUD and reports.
 * USER: read-only employee directory; the Reports menu is hidden and its API is forbidden.
 */
public enum Role {
    ADMIN,
    USER
}
