import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { Department, Employee, EmployeeRequest, EmployeeStatus, STATUS_LABELS, STATUSES } from '../core/api.models';
import { errorMessage, fieldErrors, fromIsoDate, toIsoDate } from '../core/http-utils';
import { EmployeeService } from './employee.service';
import { notBlank, notInFuture, phoneNumber, workEmail } from './employee-validators';

export interface EmployeeDialogData {
  employee: Employee | null;
  departments: Department[];
}

@Component({
  selector: 'app-employee-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatDatepickerModule],
  template: `
    <h2 mat-dialog-title>{{ employee ? 'Edit employee' : 'Add employee' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content>
        <p class="muted">Keep your team’s details up to date. All fields are required.</p>
        <div class="form-grid">
          <mat-form-field>
            <mat-label>Full name</mat-label>
            <input matInput formControlName="name" autocomplete="name" maxlength="80">
            <mat-error>{{ messageFor('name') }}</mat-error>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Work email</mat-label>
            <input matInput formControlName="email" type="email" autocomplete="email" maxlength="254">
            <mat-error>{{ messageFor('email') }}</mat-error>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Phone</mat-label>
            <input matInput formControlName="phone" type="tel" autocomplete="tel" maxlength="20">
            <mat-error>{{ messageFor('phone') }}</mat-error>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Job title</mat-label>
            <input matInput formControlName="title" maxlength="80">
            <mat-error>{{ messageFor('title') }}</mat-error>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Department</mat-label>
            <mat-select formControlName="departmentId">
              @for (d of departments; track d.id) {
                <mat-option [value]="d.id">{{ d.name }}</mat-option>
              }
            </mat-select>
            <mat-error>{{ messageFor('departmentId') }}</mat-error>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              @for (s of statuses; track s) {
                <mat-option [value]="s">{{ labels[s] }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field>
            <mat-label>Join date</mat-label>
            <input matInput formControlName="joinedOn" [matDatepicker]="picker" [max]="today">
            <mat-hint>MM/DD/YYYY</mat-hint>
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
            <mat-error>{{ messageFor('joinedOn') }}</mat-error>
          </mat-form-field>
        </div>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="saving()">Cancel</button>
        <button mat-flat-button type="submit" [disabled]="form.invalid || saving()">
          {{ saving() ? 'Saving…' : employee ? 'Save changes' : 'Add employee' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class EmployeeDialogComponent {
  private readonly data = inject<EmployeeDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<EmployeeDialogComponent, Employee>);
  private readonly service = inject(EmployeeService);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly employee = this.data.employee;
  readonly departments = this.data.departments;
  readonly statuses = STATUSES;
  readonly labels = STATUS_LABELS;
  readonly today = new Date();
  readonly saving = signal(false);
  readonly error = signal('');

  readonly form = this.fb.group({
    name: [this.employee?.name ?? '', [Validators.required, Validators.minLength(2), Validators.maxLength(80), notBlank]],
    email: [this.employee?.email ?? '', [Validators.required, workEmail, Validators.maxLength(254)]],
    phone: [this.employee?.phone ?? '', [Validators.required, phoneNumber]],
    title: [this.employee?.title ?? '', [Validators.required, Validators.maxLength(80), notBlank]],
    departmentId: this.fb.control<number | null>(this.employee?.departmentId ?? this.departments[0]?.id ?? null, Validators.required),
    status: this.fb.control<EmployeeStatus>(this.employee?.status ?? 'ACTIVE', Validators.required),
    joinedOn: this.fb.control<Date | null>(this.employee ? fromIsoDate(this.employee.joinedOn) : new Date(),
      [Validators.required, notInFuture]),
  });

  messageFor(field: keyof typeof this.form.controls): string {
    const errors = this.form.controls[field].errors;
    if (!errors) return '';
    if (errors['server']) return errors['server'];
    if (errors['required'] || errors['blank']) return 'This field is required.';
    if (errors['matDatepickerParse']) return 'Enter a valid date.';
    if (errors['future'] || errors['matDatepickerMax']) return 'Join date cannot be in the future.';
    if (errors['email']) return 'Enter a valid email address.';
    if (errors['phone']) return 'Use 7–20 digits, spaces or + ( ) . -';
    if (errors['minlength']) return `Enter at least ${errors['minlength'].requiredLength} characters.`;
    if (errors['maxlength']) return `Use at most ${errors['maxlength'].requiredLength} characters.`;
    return 'Invalid value.';
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const v = this.form.getRawValue();
    const request: EmployeeRequest = {
      name: v.name.trim(),
      email: v.email.trim(),
      phone: v.phone.trim(),
      title: v.title.trim(),
      departmentId: v.departmentId!,
      status: v.status,
      joinedOn: toIsoDate(v.joinedOn!),
    };
    this.saving.set(true);
    this.error.set('');
    const call = this.employee ? this.service.update(this.employee.id, request) : this.service.create(request);
    call.subscribe({
      next: (saved) => this.ref.close(saved),
      error: (err) => {
        this.saving.set(false);
        const fields = fieldErrors(err);
        let mapped = false;
        for (const [field, message] of Object.entries(fields)) {
          const control = this.form.get(field);
          if (control) {
            control.setErrors({ server: message });
            control.markAsTouched();
            mapped = true;
          }
        }
        this.error.set(mapped ? '' : errorMessage(err, 'Unable to save the employee.'));
      },
    });
  }
}
