import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Rejects values made only of whitespace (Validators.required accepts "   "). */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { blank: true }
    : null;

/** 7–20 characters of digits, spaces and + ( ) . - with at least 7 digits. */
export const phoneNumber: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = (control.value ?? '').toString().trim();
  if (!value) return null;
  const digits = value.replace(/\D/g, '').length;
  return /^[+0-9() .-]{7,20}$/.test(value) && digits >= 7 ? null : { phone: true };
};

/** Stricter than Validators.email: requires a dot in the domain (e.g. name@company.com). */
export const workEmail: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = (control.value ?? '').toString().trim();
  return !value || /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value) ? null : { email: true };
};

/** The date (a Date object from the datepicker) must not be after today. */
export const notInFuture: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value as Date | null;
  if (!(value instanceof Date)) return null;
  const endOfToday = new Date();
  endOfToday.setHours(23, 59, 59, 999);
  return value.getTime() > endOfToday.getTime() ? { future: true } : null;
};
