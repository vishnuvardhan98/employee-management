package com.peopledesk.department;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    public record DepartmentResponse(Long id, String name) {
    }

    private final DepartmentRepository departments;

    public DepartmentController(DepartmentRepository departments) {
        this.departments = departments;
    }

    @GetMapping
    public List<DepartmentResponse> list() {
        return departments.findAllByOrderByNameAsc().stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .toList();
    }
}
