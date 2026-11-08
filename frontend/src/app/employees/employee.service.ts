import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import {
  Department,
  Employee,
  EmployeeRequest,
  EmployeeStats,
  EmployeeStatus,
  Page,
  PageQuery,
} from '../core/api.models';
import { toParams } from '../core/http-utils';

export interface EmployeeQuery extends PageQuery {
  search: string;
  departmentId: number | null;
  status: EmployeeStatus | null;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/employees';

  /** Departments rarely change, so one request is shared for the app's lifetime. */
  readonly departments$: Observable<Department[]> = this.http
    .get<Department[]>('/api/departments')
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  list(query: EmployeeQuery): Observable<Page<Employee>> {
    return this.http.get<Page<Employee>>(this.api, { params: toParams({ ...query }) });
  }

  stats(): Observable<EmployeeStats> {
    return this.http.get<EmployeeStats>(`${this.api}/stats`);
  }

  create(employee: EmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.api, employee);
  }

  update(id: number, employee: EmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.api}/${id}`, employee);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
