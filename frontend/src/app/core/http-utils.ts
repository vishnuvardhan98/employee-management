import { HttpErrorResponse, HttpParams } from '@angular/common/http';
import { ApiError } from './api.models';

/** Builds query params, skipping null/undefined/empty values. */
export function toParams(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}

function apiError(err: unknown): Partial<ApiError> | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'message' in err.error) {
    return err.error as Partial<ApiError>;
  }
  return null;
}

/** A user-friendly message for any failed API call. */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (err instanceof HttpErrorResponse && err.status === 0) {
    return 'Unable to reach the server. Check that the backend is running and try again.';
  }
  return apiError(err)?.message || fallback;
}

/** Server-side validation messages keyed by field name (400 / 409 responses). */
export function fieldErrors(err: unknown): Record<string, string> {
  return apiError(err)?.fieldErrors ?? {};
}

/** Local calendar date as yyyy-MM-dd (no UTC shift). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function fromIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Triggers a browser download for a Blob. */
export function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Reads the filename from a Content-Disposition header. */
export function fileNameFrom(contentDisposition: string | null, fallback: string): string {
  const match = contentDisposition?.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i);
  return match ? decodeURIComponent(match[1]) : fallback;
}
