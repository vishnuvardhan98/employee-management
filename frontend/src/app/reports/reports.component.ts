import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { catchError, debounceTime, filter as rxFilter, of, switchMap, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import {
  Employee,
  EmployeeSortKey,
  EmployeeStatus,
  Page,
  PageQuery,
  ReportSummary,
  STATUS_LABELS,
  STATUSES,
  emptyPage,
} from '../core/api.models';
import { errorMessage, saveBlob, toIsoDate } from '../core/http-utils';
import { EmployeeService } from '../employees/employee.service';
import { EmployeeTableComponent } from '../shared/employee-table.component';
import { ExportFormat, ReportFilter, ReportService } from './report.service';

const firstOfMonth = () => new Date(new Date().getFullYear(), new Date().getMonth(), 1);

/** from ≤ to (the range picker also checks this; this covers typed input). */
function orderedRange(group: AbstractControl): ValidationErrors | null {
  const { start, end } = group.value as { start: Date | null; end: Date | null };
  return start && end && start.getTime() > end.getTime() ? { range: true } : null;
}

@Component({
  selector: 'app-reports',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, EmployeeTableComponent],
  template: `
    <div class="page-heading">
      <div>
        <span class="eyebrow">TEAM INSIGHTS</span>
        <h1>Good data. Clearer decisions.</h1>
        <p class="muted">Explore your employee records and take the insights with you.</p>
      </div>
      <div class="button-group">
        <button mat-stroked-button [disabled]="!canExport()" (click)="download('excel')">
          {{ exporting() === 'excel' ? 'Preparing…' : '↓ Export Excel' }}
        </button>
        <button mat-flat-button [disabled]="!canExport()" (click)="download('pdf')">
          {{ exporting() === 'pdf' ? 'Preparing…' : '↓ Export PDF' }}
        </button>
      </div>
    </div>

    <section class="panel report-filters">
      <div class="panel-heading">
        <div>
          <h2>Build your report</h2>
          <p class="muted">Filter by join date, department, and employee status.</p>
        </div>
        <button mat-button type="button" (click)="reset()">Reset to this month</button>
      </div>
      <form class="filter-bar" [formGroup]="form">
        <mat-form-field class="range-field">
          <mat-label>Join date range</mat-label>
          <mat-date-range-input [formGroup]="form.controls.range" [rangePicker]="rangePicker">
            <input matStartDate formControlName="start" placeholder="From" aria-label="From date">
            <input matEndDate formControlName="end" placeholder="To" aria-label="To date">
          </mat-date-range-input>
          <mat-hint>MM/DD/YYYY – MM/DD/YYYY</mat-hint>
          <mat-datepicker-toggle matIconSuffix [for]="rangePicker" />
          <mat-date-range-picker #rangePicker />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Department</mat-label>
          <mat-select formControlName="departmentId">
            <mat-option [value]="null">All departments</mat-option>
            @for (d of departments(); track d.id) {
              <mat-option [value]="d.id">{{ d.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option [value]="null">All statuses</mat-option>
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ labels[s] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </form>
      @if (rangeError()) {
        <p class="error filter-error" role="alert">{{ rangeError() }}</p>
      }
    </section>

    <section class="stats-grid" aria-label="Report summary">
      <article class="stat-card">
        <span>Matching employees</span>
        <div>{{ summary()?.total ?? '–' }}<span class="stat-icon">◫</span></div>
        <small>Joined within the selected dates</small>
      </article>
      @for (s of statuses; track s) {
        <article class="stat-card">
          <span>{{ labels[s] }}</span>
          <div>{{ summary() ? statusCount(s) : '–' }}</div>
          <small>{{ percent(statusCount(s)) }} of filtered results</small>
        </article>
      }
    </section>

    @if (summary(); as s) {
      <section class="insight-grid">
        <article class="panel insight">
          <h2>By department</h2>
          @for (d of s.byDepartment; track d.key) {
            <div class="bar-row">
              <span>{{ d.key }}</span>
              <div class="bar"><span [style.width.%]="share(d.count, maxDepartment())"></span></div>
              <strong>{{ d.count }}</strong>
            </div>
          } @empty {
            <p class="muted">No data for these filters.</p>
          }
        </article>
        <article class="panel insight">
          <h2>Joined by month</h2>
          @for (m of s.byJoinMonth; track m.label) {
            <div class="bar-row">
              <span>{{ monthName(m.year, m.month) }}</span>
              <div class="bar"><span [style.width.%]="share(m.count, maxMonth())"></span></div>
              <strong>{{ m.count }}</strong>
            </div>
          } @empty {
            <p class="muted">No data for these filters.</p>
          }
        </article>
      </section>
    }

    <section class="panel">
      <div class="panel-heading">
        <div>
          <h2>Employee report <span class="count-badge">{{ page().totalElements }}</span></h2>
          <p class="muted">Exports include every filtered record, across all pages.</p>
        </div>
      </div>
      <app-employee-table
        [page]="page()" [loading]="loading()"
        [sortActive]="pageQuery().sort" [sortDirection]="pageQuery().direction"
        (sortChange)="onSort($event)" (pageChange)="onPage($event)" />
    </section>
  `,
})
export class ReportsComponent {
  private readonly reports = inject(ReportService);
  private readonly snack = inject(MatSnackBar);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly statuses = STATUSES;
  readonly labels = STATUS_LABELS;
  readonly departments = toSignal(inject(EmployeeService).departments$.pipe(catchError(() => of([]))), {
    initialValue: [],
  });

  readonly form = this.fb.group({
    range: this.fb.group(
      {
        start: this.fb.control<Date | null>(firstOfMonth(), Validators.required),
        end: this.fb.control<Date | null>(new Date(), Validators.required),
      },
      { validators: orderedRange },
    ),
    departmentId: this.fb.control<number | null>(null),
    status: this.fb.control<EmployeeStatus | null>(null),
  });

  /** The last valid filter; null while the form is invalid. */
  readonly filter = signal<ReportFilter | null>(this.currentFilter());
  readonly pageQuery = signal<PageQuery>({ page: 0, size: 10, sort: 'joined', direction: 'desc' });
  readonly page = signal<Page<Employee>>(emptyPage());
  readonly summary = signal<ReportSummary | null>(null);
  readonly loading = signal(true);
  readonly exporting = signal<ExportFormat | null>(null);
  readonly rangeError = signal('');

  readonly canExport = computed(() => !!this.filter() && !this.exporting() && this.page().totalElements > 0);
  readonly maxDepartment = computed(() => Math.max(1, ...(this.summary()?.byDepartment.map((d) => d.count) ?? [])));
  readonly maxMonth = computed(() => Math.max(1, ...(this.summary()?.byJoinMonth.map((m) => m.count) ?? [])));
  private readonly request = computed(() => ({ filter: this.filter(), query: this.pageQuery() }));

  constructor() {
    this.form.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.rangeError.set(this.validationMessage());
      this.filter.set(this.currentFilter());
      this.pageQuery.update((q) => ({ ...q, page: 0 }));
    });

    toObservable(this.request)
      .pipe(
        rxFilter(({ filter }) => {
          if (!filter) {
            this.page.set(emptyPage());
            this.loading.set(false);
          }
          return !!filter;
        }),
        tap(() => this.loading.set(true)),
        switchMap(({ filter, query }) =>
          this.reports.rows(filter!, query).pipe(
            catchError((err) => {
              this.snack.open(errorMessage(err, 'Unable to load the report.'), 'Close', { duration: 5000 });
              return of(emptyPage<Employee>(query.size));
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.page.set(page);
        this.loading.set(false);
      });

    toObservable(this.filter)
      .pipe(
        switchMap((filter) => (filter ? this.reports.summary(filter).pipe(catchError(() => of(null))) : of(null))),
        takeUntilDestroyed(),
      )
      .subscribe((summary) => this.summary.set(summary));
  }

  statusCount(status: EmployeeStatus): number {
    return this.summary()?.byStatus.find((s) => s.key === status)?.count ?? 0;
  }

  percent(count: number): string {
    const total = this.summary()?.total ?? 0;
    return total ? `${Math.round((count / total) * 100)}%` : '0%';
  }

  share(count: number, max: number): number {
    return Math.max(4, (count / max) * 100);
  }

  monthName(year: number, month: number): string {
    return new Date(year, month - 1, 1).toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
  }

  onSort(sort: Sort): void {
    this.pageQuery.update((q) => ({
      ...q,
      page: 0,
      sort: sort.active as EmployeeSortKey,
      direction: sort.direction || 'asc',
    }));
  }

  onPage(event: PageEvent): void {
    this.pageQuery.update((q) => ({ ...q, page: event.pageIndex, size: event.pageSize }));
  }

  reset(): void {
    this.form.setValue({ range: { start: firstOfMonth(), end: new Date() }, departmentId: null, status: null });
  }

  download(format: ExportFormat): void {
    const filter = this.filter();
    if (!filter || !this.canExport()) return;
    this.exporting.set(format);
    const { sort, direction } = this.pageQuery();
    this.reports.export(format, filter, { sort, direction }).subscribe({
      next: ({ blob, fileName }) => {
        saveBlob(blob, fileName);
        this.exporting.set(null);
      },
      error: (err) => {
        this.exporting.set(null);
        this.snack.open(errorMessage(err, 'Export failed. Please try again.'), 'Close', { duration: 5000 });
      },
    });
  }

  private currentFilter(): ReportFilter | null {
    if (this.form.invalid) return null;
    const { range, departmentId, status } = this.form.getRawValue();
    return { from: toIsoDate(range.start!), to: toIsoDate(range.end!), departmentId, status };
  }

  private validationMessage(): string {
    const range = this.form.controls.range;
    const { start, end } = range.controls;
    if (start.hasError('matDatepickerParse') || end.hasError('matDatepickerParse')) return 'Enter valid dates.';
    if (start.hasError('required') || end.hasError('required')) return 'Choose both a start and an end date.';
    if (range.hasError('range') || start.hasError('matStartDateInvalid') || end.hasError('matEndDateInvalid')) {
      return 'The start date must be on or before the end date.';
    }
    return '';
  }
}
