import { Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Employee, EmployeeStatus, Page, STATUS_LABELS } from '../core/api.models';

/**
 * Presentational, server-driven table: it shows one page from the API and emits sort/page
 * events; the parent fetches the next page. Column ids double as the API sort keys.
 */
@Component({
  selector: 'app-employee-table',
  imports: [DatePipe, MatTableModule, MatPaginatorModule, MatSortModule, MatButtonModule, MatProgressBarModule],
  template: `
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" aria-label="Loading employees" />
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="page().content" matSort [matSortActive]="sortActive()"
             [matSortDirection]="sortDirection()" matSortDisableClear (matSortChange)="sortChange.emit($event)">

        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Employee</th>
          <td mat-cell *matCellDef="let e">
            <div class="person-cell">
              <span class="avatar" [class.lilac]="e.id % 3 === 0" [class.peach]="e.id % 3 === 1">{{ initials(e.name) }}</span>
              <div><strong>{{ e.name }}</strong><small>{{ e.email }}</small></div>
            </div>
          </td>
        </ng-container>

        <ng-container matColumnDef="phone">
          <th mat-header-cell *matHeaderCellDef>Phone</th>
          <td mat-cell *matCellDef="let e">{{ e.phone }}</td>
        </ng-container>

        <ng-container matColumnDef="department">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Department</th>
          <td mat-cell *matCellDef="let e">{{ e.departmentName }}</td>
        </ng-container>

        <ng-container matColumnDef="title">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Job title</th>
          <td mat-cell *matCellDef="let e">{{ e.title }}</td>
        </ng-container>

        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
          <td mat-cell *matCellDef="let e">
            <span class="status-pill" [class.leave]="e.status === 'ON_LEAVE'" [class.inactive]="e.status === 'INACTIVE'">
              ● {{ statusLabel(e.status) }}
            </span>
          </td>
        </ng-container>

        <ng-container matColumnDef="joined">
          <th mat-header-cell *matHeaderCellDef mat-sort-header>Join date</th>
          <td mat-cell *matCellDef="let e">{{ e.joinedOn | date: 'MMM d, y' }}</td>
        </ng-container>

        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef>Actions</th>
          <td mat-cell *matCellDef="let e">
            <button mat-button type="button" (click)="$event.stopPropagation(); edit.emit(e)" [attr.aria-label]="'Edit ' + e.name">Edit</button>
            <button mat-button type="button" class="delete-link" (click)="$event.stopPropagation(); remove.emit(e)" [attr.aria-label]="'Delete ' + e.name">Delete</button>
          </td>
        </ng-container>

        <tr mat-header-row *matHeaderRowDef="columns()"></tr>
        <tr mat-row *matRowDef="let row; columns: columns()"
            [class.editable-row]="editable()"
            [attr.tabindex]="editable() ? 0 : null"
            [attr.aria-label]="editable() ? 'Edit ' + row.name : null"
            (click)="editable() && edit.emit(row)"
            (keydown.enter)="editable() && edit.emit(row)"></tr>
        <tr *matNoDataRow>
          <td [attr.colspan]="columns().length" class="empty-state">
            @if (loading()) {
              <strong>Loading…</strong>
            } @else {
              <strong>No employees found</strong>
              <p>Try a different search or clear your filters.</p>
            }
          </td>
        </tr>
      </table>
    </div>
    <mat-paginator [length]="page().totalElements" [pageIndex]="page().page" [pageSize]="page().size"
                   [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons aria-label="Employee pagination"
                   (page)="pageChange.emit($event)" />
  `,
})
export class EmployeeTableComponent {
  readonly page = input.required<Page<Employee>>();
  readonly sortActive = input<string>('name');
  readonly sortDirection = input<'asc' | 'desc'>('asc');
  readonly editable = input(false);
  readonly showPhone = input(false);
  readonly loading = input(false);

  readonly sortChange = output<Sort>();
  readonly pageChange = output<PageEvent>();
  readonly edit = output<Employee>();
  readonly remove = output<Employee>();

  readonly columns = computed(() => [
    'name',
    ...(this.showPhone() ? ['phone'] : []),
    'department',
    'title',
    'status',
    'joined',
    ...(this.editable() ? ['actions'] : []),
  ]);

  statusLabel(status: EmployeeStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .map((part) => part[0].toUpperCase())
      .slice(0, 2)
      .join('');
  }
}
