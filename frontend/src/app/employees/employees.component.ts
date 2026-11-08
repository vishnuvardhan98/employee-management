import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { AuthService } from '../core/auth.service';
import {
  Employee,
  EmployeeSortKey,
  EmployeeStats,
  Page,
  STATUS_LABELS,
  STATUSES,
  emptyPage,
} from '../core/api.models';
import { errorMessage } from '../core/http-utils';
import { EmployeeTableComponent } from '../shared/employee-table.component';
import { EmployeeDialogComponent, EmployeeDialogData } from './employee-dialog.component';
import { DeleteDialogComponent } from './delete-dialog.component';
import { EmployeeQuery, EmployeeService } from './employee.service';

const INITIAL_QUERY: EmployeeQuery = {
  page: 0,
  size: 10,
  sort: 'name',
  direction: 'asc',
  search: '',
  departmentId: null,
  status: null,
};

@Component({
  selector: 'app-employees',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule, MatInputModule,
    EmployeeTableComponent],
  template: `
    <div class="page-heading">
      <div>
        <span class="eyebrow">PEOPLE OPERATIONS</span>
        <h1>Your people, at a glance.</h1>
        <p class="muted">A home for your team and the details that matter.</p>
      </div>
      @if (auth.isAdmin()) {
        <button mat-flat-button (click)="openEditor()">＋ Add employee</button>
      }
    </div>

    <section class="stats-grid" aria-label="Employee statistics">
      <article class="stat-card">
        <span>Total employees</span>
        <div>{{ stats()?.total ?? '–' }}<span class="stat-icon">◫</span></div>
        <small>Your entire team, together</small>
      </article>
      <article class="stat-card">
        <span>Active employees</span>
        <div>{{ stats()?.active ?? '–' }}<span class="stat-icon mint">↗</span></div>
        <small><span class="green-dot">●</span> Ready to make things happen</small>
      </article>
      <article class="stat-card">
        <span>Departments</span>
        <div>{{ stats()?.departments ?? '–' }}<span class="stat-icon lilac">▦</span></div>
        <small>Different strengths. One team.</small>
      </article>
      <article class="stat-card">
        <span>On leave</span>
        <div>{{ stats()?.onLeave ?? '–' }}<span class="stat-icon peach">☀</span></div>
        <small>Taking a well-deserved pause</small>
      </article>
    </section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <h2>Employee directory <span class="count-badge">{{ page().totalElements }}</span></h2>
          <p class="muted">
            @if (auth.isAdmin()) {
              Manage and keep your team’s information up to date.
            } @else {
              You have read-only access to the directory.
            }
          </p>
        </div>
      </div>

      <div class="filter-bar">
        <mat-form-field class="search-field">
          <mat-label>Search name, email or job title</mat-label>
          <input matInput [formControl]="search" maxlength="100">
          @if (search.value) {
            <button matSuffix mat-button type="button" (click)="clearSearch()" aria-label="Clear search">✕</button>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Department</mat-label>
          <mat-select [value]="query().departmentId" (selectionChange)="patch({ departmentId: $event.value })">
            <mat-option [value]="null">All departments</mat-option>
            @for (d of departments(); track d.id) {
              <mat-option [value]="d.id">{{ d.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Status</mat-label>
          <mat-select [value]="query().status" (selectionChange)="patch({ status: $event.value })">
            <mat-option [value]="null">All statuses</mat-option>
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ labels[s] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <button mat-button type="button" (click)="clearFilters()">Clear filters</button>
      </div>

      <app-employee-table
        [page]="page()" [loading]="loading()" [editable]="auth.isAdmin()" [showPhone]="true"
        [sortActive]="query().sort" [sortDirection]="query().direction"
        (sortChange)="onSort($event)" (pageChange)="onPage($event)"
        (edit)="openEditor($event)" (remove)="confirmDelete($event)" />
    </section>
    @if (auth.isAdmin()) {
      <p class="table-hint">Tip: select an employee row to edit their details.</p>
    }
  `,
})
export class EmployeesComponent {
  readonly auth = inject(AuthService);
  private readonly service = inject(EmployeeService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly statuses = STATUSES;
  readonly labels = STATUS_LABELS;
  readonly search = new FormControl('', { nonNullable: true });

  readonly query = signal<EmployeeQuery>(INITIAL_QUERY);
  readonly page = signal<Page<Employee>>(emptyPage());
  readonly loading = signal(true);
  readonly stats = signal<EmployeeStats | null>(null);
  readonly departments = toSignal(this.service.departments$.pipe(catchError(() => of([]))), { initialValue: [] });

  constructor() {
    // Every query change (filter, sort, page, reload) fetches a page; switchMap drops stale responses.
    toObservable(this.query)
      .pipe(
        tap(() => this.loading.set(true)),
        switchMap((q) =>
          this.service.list(q).pipe(
            catchError((err) => {
              this.snack.open(errorMessage(err, 'Unable to load employees.'), 'Close', { duration: 5000 });
              return of(emptyPage<Employee>(q.size));
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.page.set(page);
        this.loading.set(false);
      });

    // Real-time search, debounced by 300 ms.
    this.search.valueChanges
      .pipe(map((v) => v.trim()), debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((search) => this.patch({ search }));

    this.loadStats();
  }

  /** Changing any filter returns to the first page. */
  patch(changes: Partial<EmployeeQuery>): void {
    this.query.update((q) => ({ ...q, page: 0, ...changes }));
  }

  onSort(sort: Sort): void {
    this.patch({ sort: sort.active as EmployeeSortKey, direction: sort.direction || 'asc' });
  }

  onPage(event: PageEvent): void {
    this.query.update((q) => ({ ...q, page: event.pageIndex, size: event.pageSize }));
  }

  clearSearch(): void {
    this.search.setValue('', { emitEvent: false });
    this.patch({ search: '' });
  }

  clearFilters(): void {
    this.search.setValue('', { emitEvent: false });
    this.patch({ search: '', departmentId: null, status: null });
  }

  openEditor(employee?: Employee): void {
    if (!this.auth.isAdmin()) return;
    const data: EmployeeDialogData = { employee: employee ?? null, departments: this.departments() };
    this.dialog
      .open(EmployeeDialogComponent, { data, width: '660px', maxWidth: '95vw', autoFocus: 'first-tabbable' })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((saved?: Employee) => {
        if (!saved) return;
        this.snack.open(employee ? `${saved.name} was updated.` : `${saved.name} was added.`, 'Close', { duration: 3000 });
        this.reload();
      });
  }

  confirmDelete(employee: Employee): void {
    this.dialog
      .open(DeleteDialogComponent, { data: employee, width: '440px', maxWidth: '95vw' })
      .afterClosed()
      .pipe(
        switchMap((confirmed) => (confirmed ? this.service.delete(employee.id).pipe(map(() => true)) : of(false))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (deleted) => {
          if (!deleted) return;
          this.snack.open(`${employee.name} was deleted.`, 'Close', { duration: 3000 });
          // If the last row on a page was removed, step back one page.
          const { content, page } = this.page();
          this.query.update((q) => ({ ...q, page: content.length === 1 && page > 0 ? page - 1 : q.page }));
          this.loadStats();
        },
        error: (err) => this.snack.open(errorMessage(err, 'Unable to delete the employee.'), 'Close', { duration: 5000 }),
      });
  }

  private reload(): void {
    this.query.update((q) => ({ ...q })); // new object reference re-triggers the fetch
    this.loadStats();
  }

  private loadStats(): void {
    this.service
      .stats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (s) => this.stats.set(s), error: () => this.stats.set(null) });
  }
}
