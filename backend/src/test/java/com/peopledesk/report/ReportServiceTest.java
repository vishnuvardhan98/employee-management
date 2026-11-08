package com.peopledesk.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.peopledesk.common.BadRequestException;
import com.peopledesk.department.DepartmentRepository;
import com.peopledesk.employee.EmployeeRepository;
import com.peopledesk.employee.EmployeeStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private final ReportService service = new ReportService(
            mock(EmployeeRepository.class), mock(DepartmentRepository.class),
            Clock.fixed(Instant.parse("2026-11-08T10:00:00Z"), ZoneOffset.UTC), 100);

    @Test
    void defaultsToCurrentMonth() {
        ReportFilter filter = service.filter(null, null, null, null);

        assertThat(filter.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(filter.to()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void keepsExplicitRangeAndFilters() {
        ReportFilter filter = service.filter(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 2L, EmployeeStatus.ON_LEAVE);

        assertThat(filter).isEqualTo(new ReportFilter(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 2L, EmployeeStatus.ON_LEAVE));
    }

    @Test
    void rejectsFromAfterTo() {
        assertThatThrownBy(() -> service.filter(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null))
                .isInstanceOf(BadRequestException.class);
    }
}
