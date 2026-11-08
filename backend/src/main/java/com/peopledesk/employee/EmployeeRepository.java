package com.peopledesk.employee;

import com.peopledesk.report.CategoryCount;
import com.peopledesk.report.MonthCount;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /** Fetches the department in the same query to avoid N+1 selects when mapping a page. */
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "department")
    List<Employee> findAll(Specification<Employee> spec, Sort sort);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    // ---- Aggregates (COUNT / GROUP BY) for the directory cards and the report summary ----

    @Query("select count(distinct e.department.id) from Employee e")
    long countDistinctDepartments();

    @Query("""
            select new com.peopledesk.report.CategoryCount(cast(e.status as string), count(e))
            from Employee e
            where (:from is null or e.joinedOn >= :from)
              and (:to is null or e.joinedOn <= :to)
              and (:departmentId is null or e.department.id = :departmentId)
              and (:status is null or e.status = :status)
            group by e.status
            """)
    List<CategoryCount> countByStatus(LocalDate from, LocalDate to, Long departmentId, EmployeeStatus status);

    @Query("""
            select new com.peopledesk.report.CategoryCount(d.name, count(e))
            from Employee e join e.department d
            where (:from is null or e.joinedOn >= :from)
              and (:to is null or e.joinedOn <= :to)
              and (:departmentId is null or d.id = :departmentId)
              and (:status is null or e.status = :status)
            group by d.name
            order by count(e) desc, d.name
            """)
    List<CategoryCount> countByDepartment(LocalDate from, LocalDate to, Long departmentId, EmployeeStatus status);

    @Query("""
            select new com.peopledesk.report.MonthCount(year(e.joinedOn), month(e.joinedOn), count(e))
            from Employee e
            where (:from is null or e.joinedOn >= :from)
              and (:to is null or e.joinedOn <= :to)
              and (:departmentId is null or e.department.id = :departmentId)
              and (:status is null or e.status = :status)
            group by year(e.joinedOn), month(e.joinedOn)
            order by year(e.joinedOn), month(e.joinedOn)
            """)
    List<MonthCount> countByJoinMonth(LocalDate from, LocalDate to, Long departmentId, EmployeeStatus status);
}
