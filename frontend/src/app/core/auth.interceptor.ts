import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const PUBLIC_ENDPOINTS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];
const SESSION_EXPIRED = 'Your session has expired. Please sign in again.';

const withToken = (req: HttpRequest<unknown>, token: string) =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

/**
 * Attaches the JWT to same-origin /api requests. If the access token has expired (or the API
 * answers 401) it refreshes once and retries; if that fails the user is signed out.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || PUBLIC_ENDPOINTS.some((url) => req.url.startsWith(url))) {
    return next(req);
  }
  const auth = inject(AuthService);
  const token = auth.accessToken();
  if (!token) {
    return next(req);
  }

  const refreshAndRetry = () =>
    auth.refresh().pipe(
      switchMap((fresh) => next(withToken(req, fresh))),
      catchError((err) => {
        auth.logout(SESSION_EXPIRED);
        return throwError(() => err);
      }),
    );

  // Proactive refresh avoids a guaranteed 401 round trip.
  if (auth.accessTokenExpired()) {
    return refreshAndRetry();
  }

  return next(withToken(req, token)).pipe(
    catchError((err: unknown) =>
      err instanceof HttpErrorResponse && err.status === 401 ? refreshAndRetry() : throwError(() => err),
    ),
  );
};
