package com.peopledesk.employee;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String phone,
        String title,
        Long departmentId,
        String departmentName,
        EmployeeStatus status,
        LocalDate joinedOn) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getName(),
                e.getEmail(),
                e.getPhone(),
                e.getTitle(),
                e.getDepartment().getId(),
                e.getDepartment().getName(),
                e.getStatus(),
                e.getJoinedOn());
    }
}
