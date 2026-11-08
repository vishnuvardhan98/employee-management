package com.peopledesk.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request body for creating or updating an employee (server-side validation mirrors the form). */
public record EmployeeRequest(
        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 80, message = "Name must be 2–80 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must be at most 254 characters.")
        String email,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "^[+0-9() .-]{7,20}$", message = "Enter a valid phone number (7–20 digits or + ( ) . - characters).")
        String phone,

        @NotBlank(message = "Job title is required.")
        @Size(max = 80, message = "Job title must be at most 80 characters.")
        String title,

        @NotNull(message = "Department is required.")
        Long departmentId,

        @NotNull(message = "Status is required.")
        EmployeeStatus status,

        @NotNull(message = "Join date is required.")
        @PastOrPresent(message = "Join date cannot be in the future.")
        LocalDate joinedOn) {
}
