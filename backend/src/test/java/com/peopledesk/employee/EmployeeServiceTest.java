package com.peopledesk.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peopledesk.common.DuplicateFieldException;
import com.peopledesk.common.InvalidFieldException;
import com.peopledesk.common.ResourceNotFoundException;
import com.peopledesk.department.Department;
import com.peopledesk.department.DepartmentRepository;
import com.peopledesk.report.CategoryCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employees;
    @Mock
    private DepartmentRepository departments;
    @InjectMocks
    private EmployeeService service;

    private Department engineering;

    @BeforeEach
    void setUp() {
        engineering = new Department("Engineering");
        ReflectionTestUtils.setField(engineering, "id", 1L);
    }

    private static EmployeeRequest request(String email) {
        return new EmployeeRequest("  Ada Lovelace ", email, " 202-555-0199 ", " Engineer ", 1L,
                EmployeeStatus.ACTIVE, LocalDate.of(2026, 1, 5));
    }

    @Test
    void createTrimsFieldsAndNormalizesEmail() {
        when(employees.existsByEmail("ada@example.com")).thenReturn(false);
        when(departments.findById(1L)).thenReturn(Optional.of(engineering));
        when(employees.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse created = service.create(request("  ADA@Example.com "));

        assertThat(created.name()).isEqualTo("Ada Lovelace");
        assertThat(created.email()).isEqualTo("ada@example.com");
        assertThat(created.phone()).isEqualTo("202-555-0199");
        assertThat(created.title()).isEqualTo("Engineer");
        assertThat(created.departmentName()).isEqualTo("Engineering");
    }

    @Test
    void createRejectsDuplicateEmailIgnoringCase() {
        when(employees.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Ada@Example.com")))
                .isInstanceOf(DuplicateFieldException.class)
                .extracting("field").isEqualTo("email");
        verify(employees, never()).save(any());
    }

    @Test
    void createRejectsUnknownDepartment() {
        when(employees.existsByEmail("ada@example.com")).thenReturn(false);
        when(departments.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("ada@example.com")))
                .isInstanceOf(InvalidFieldException.class)
                .extracting("field").isEqualTo("departmentId");
    }

    @Test
    void updateAllowsKeepingOwnEmailButChecksOthers() {
        Employee existing = new Employee();
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(employees.findById(7L)).thenReturn(Optional.of(existing));
        when(employees.existsByEmailAndIdNot("ada@example.com", 7L)).thenReturn(false);
        when(departments.findById(1L)).thenReturn(Optional.of(engineering));
        when(employees.save(existing)).thenReturn(existing);

        EmployeeResponse updated = service.update(7L, request("ada@example.com"));

        assertThat(updated.id()).isEqualTo(7L);
        assertThat(updated.email()).isEqualTo("ada@example.com");
    }

    @Test
    void updateMissingEmployeeThrowsNotFound() {
        when(employees.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("ada@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesExistingEmployee() {
        Employee existing = new Employee();
        when(employees.findById(3L)).thenReturn(Optional.of(existing));

        service.delete(3L);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employees).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void statsAddsUpStatusCounts() {
        when(employees.countByStatus(null, null, null, null)).thenReturn(List.of(
                new CategoryCount("ACTIVE", 5L), new CategoryCount("ON_LEAVE", 2L)));
        when(employees.countDistinctDepartments()).thenReturn(3L);

        EmployeeStats stats = service.stats();

        assertThat(stats).isEqualTo(new EmployeeStats(7, 5, 2, 0, 3));
    }

    @Test
    void sortRejectsUnknownField() {
        assertThatThrownBy(() -> EmployeeSort.of("password", "asc"))
                .isInstanceOf(com.peopledesk.common.BadRequestException.class);
    }

    @Test
    void likeEscapingNeutralizesWildcards() {
        assertThat(EmployeeSpecifications.escapeLike("50%_off!")).isEqualTo("50!%!_off!!");
    }
}
