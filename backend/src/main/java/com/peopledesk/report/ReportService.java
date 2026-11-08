package com.peopledesk.report;

import com.peopledesk.common.BadRequestException;
import com.peopledesk.common.PageResponse;
import com.peopledesk.department.Department;
import com.peopledesk.department.DepartmentRepository;
import com.peopledesk.employee.EmployeeRepository;
import com.peopledesk.employee.EmployeeResponse;
import com.peopledesk.employee.EmployeeSpecifications;
import com.peopledesk.employee.EmployeeStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final Clock clock;
    private final int maxExportRows;

    public ReportService(EmployeeRepository employees, DepartmentRepository departments, Clock clock,
                         @Value("${app.reports.max-export-rows:10000}") int maxExportRows) {
        this.employees = employees;
        this.departments = departments;
        this.clock = clock;
        this.maxExportRows = maxExportRows;
    }

    /** Applies the default range (first of this month → today) and checks from ≤ to. */
    public ReportFilter filter(LocalDate from, LocalDate to, Long departmentId, EmployeeStatus status) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : today;
        if (start.isAfter(end)) {
            throw new BadRequestException("The 'from' date must be on or before the 'to' date.");
        }
        return new ReportFilter(start, end, departmentId, status);
    }

    public PageResponse<EmployeeResponse> rows(ReportFilter filter, Pageable pageable) {
        return PageResponse.from(
                employees.findAll(EmployeeSpecifications.matching(filter.toEmployeeFilter()), pageable),
                EmployeeResponse::from);
    }

    public ReportSummary summary(ReportFilter f) {
        List<CategoryCount> byStatus = employees.countByStatus(f.from(), f.to(), f.departmentId(), f.status());
        long total = byStatus.stream().mapToLong(CategoryCount::count).sum();
        return new ReportSummary(
                f.from(),
                f.to(),
                total,
                byStatus,
                employees.countByDepartment(f.from(), f.to(), f.departmentId(), f.status()),
                employees.countByJoinMonth(f.from(), f.to(), f.departmentId(), f.status()));
    }

    /** Every row matching the filter (not just the visible page), capped to protect the server. */
    public List<EmployeeResponse> exportRows(ReportFilter filter, Sort sort) {
        Page<EmployeeResponse> page = employees
                .findAll(EmployeeSpecifications.matching(filter.toEmployeeFilter()), PageRequest.of(0, maxExportRows, sort))
                .map(EmployeeResponse::from);
        if (page.getTotalElements() > maxExportRows) {
            throw new BadRequestException("The report has more than " + maxExportRows
                    + " rows. Narrow the filters before exporting.");
        }
        return page.getContent();
    }

    /** Human-readable description of the filters, printed on exported files. */
    public String describe(ReportFilter f) {
        String department = f.departmentId() == null
                ? "All"
                : departments.findById(f.departmentId()).map(Department::getName).orElse("Unknown");
        String status = f.status() == null ? "All" : f.status().label();
        return "Joined %s to %s  |  Department: %s  |  Status: %s".formatted(f.from(), f.to(), department, status);
    }
}
