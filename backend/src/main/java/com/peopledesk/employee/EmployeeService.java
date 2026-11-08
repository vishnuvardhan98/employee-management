package com.peopledesk.employee;

import com.peopledesk.common.DuplicateFieldException;
import com.peopledesk.common.InvalidFieldException;
import com.peopledesk.common.PageResponse;
import com.peopledesk.common.ResourceNotFoundException;
import com.peopledesk.department.Department;
import com.peopledesk.department.DepartmentRepository;
import com.peopledesk.report.CategoryCount;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    static final String DUPLICATE_EMAIL = "An employee with this email already exists.";

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments) {
        this.employees = employees;
        this.departments = departments;
    }

    public PageResponse<EmployeeResponse> search(EmployeeFilter filter, Pageable pageable) {
        return PageResponse.from(
                employees.findAll(EmployeeSpecifications.matching(filter), pageable),
                EmployeeResponse::from);
    }

    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(find(id));
    }

    public EmployeeStats stats() {
        Map<String, Long> byStatus = employees.countByStatus(null, null, null, null).stream()
                .collect(Collectors.toMap(CategoryCount::key, CategoryCount::count));
        long active = byStatus.getOrDefault(EmployeeStatus.ACTIVE.name(), 0L);
        long onLeave = byStatus.getOrDefault(EmployeeStatus.ON_LEAVE.name(), 0L);
        long inactive = byStatus.getOrDefault(EmployeeStatus.INACTIVE.name(), 0L);
        return new EmployeeStats(active + onLeave + inactive, active, onLeave, inactive,
                employees.countDistinctDepartments());
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (employees.existsByEmail(email)) {
            throw new DuplicateFieldException("email", DUPLICATE_EMAIL);
        }
        Employee employee = new Employee();
        apply(employee, request, email);
        return EmployeeResponse.from(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = find(id);
        String email = normalizeEmail(request.email());
        if (employees.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateFieldException("email", DUPLICATE_EMAIL);
        }
        apply(employee, request, email);
        return EmployeeResponse.from(employees.save(employee));
    }

    /** Hard delete: employees have no dependent records, so the row is removed permanently. */
    @Transactional
    public void delete(Long id) {
        employees.delete(find(id));
    }

    private Employee find(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private void apply(Employee employee, EmployeeRequest request, String email) {
        Department department = departments.findById(request.departmentId())
                .orElseThrow(() -> new InvalidFieldException("departmentId", "Choose a valid department."));
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setPhone(request.phone().trim());
        employee.setTitle(request.title().trim());
        employee.setDepartment(department);
        employee.setStatus(request.status());
        employee.setJoinedOn(request.joinedOn());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
