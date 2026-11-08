import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Employee, EmployeeStatus, Page, PageQuery, ReportSummary } from '../core/api.models';
import { fileNameFrom, toParams } from '../core/http-utils';

export interface ReportFilter {
  from: string; // yyyy-MM-dd
  to: string;
  departmentId: number | null;
  status: EmployeeStatus | null;
}

export type ExportFormat = 'excel' | 'pdf';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/reports';

  rows(filter: ReportFilter, query: PageQuery): Observable<Page<Employee>> {
    return this.http.get<Page<Employee>>(`${this.api}/employees`, { params: toParams({ ...filter, ...query }) });
  }

  summary(filter: ReportFilter): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(`${this.api}/summary`, { params: toParams({ ...filter }) });
  }

  /** The server builds the file from every filtered row (not just the visible page). */
  export(format: ExportFormat, filter: ReportFilter, sort: Pick<PageQuery, 'sort' | 'direction'>):
    Observable<{ blob: Blob; fileName: string }> {
    return this.http
      .get(`${this.api}/export/${format}`, {
        params: toParams({ ...filter, ...sort }),
        responseType: 'blob',
        observe: 'response',
      })
      .pipe(
        map((response) => ({
          blob: response.body ?? new Blob(),
          fileName: fileNameFrom(
            response.headers.get('Content-Disposition'),
            `employee-report.${format === 'excel' ? 'xlsx' : 'pdf'}`,
          ),
        })),
      );
  }
}
