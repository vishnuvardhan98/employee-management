import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { AuthResponse, UserInfo } from './api.models';

const KEYS = {
  accessToken: 'peopledesk.jwt',
  refreshToken: 'peopledesk.refreshToken',
  session: 'peopledesk.session',
  rememberedUsername: 'peopledesk.username',
} as const;

interface StoredSession {
  accessTokenExpiresAt: number;
  refreshTokenExpiresAt: number;
  user: UserInfo;
}

/**
 * Owns the login session. The JWT access token and refresh token are kept in localStorage
 * (as the assessment requires); the access token is short-lived and refreshed transparently by
 * the HTTP interceptor. All authorization is enforced again by the backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  private readonly session = signal<StoredSession | null>(readSession());
  private refreshInFlight$: Observable<string> | null = null;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  /** True while the refresh token (the overall session) is still valid. */
  isAuthenticated(): boolean {
    const session = this.session();
    return !!session && !!this.accessToken() && session.refreshTokenExpiresAt > Date.now();
  }

  accessToken(): string | null {
    return storage(() => localStorage.getItem(KEYS.accessToken));
  }

  accessTokenExpired(): boolean {
    const session = this.session();
    return !session || session.accessTokenExpiresAt <= Date.now() + 5_000;
  }

  rememberedUsername(): string {
    return storage(() => localStorage.getItem(KEYS.rememberedUsername)) ?? '';
  }

  login(username: string, password: string, rememberMe: boolean): Observable<UserInfo> {
    return this.http.post<AuthResponse>('/api/auth/login', { username, password, rememberMe }).pipe(
      tap((response) => {
        this.store(response);
        storage(() =>
          rememberMe
            ? localStorage.setItem(KEYS.rememberedUsername, username)
            : localStorage.removeItem(KEYS.rememberedUsername),
        );
      }),
      map((response) => response.user),
    );
  }

  /** Exchanges the refresh token for a new token pair. Concurrent callers share one request. */
  refresh(): Observable<string> {
    const refreshToken = storage(() => localStorage.getItem(KEYS.refreshToken));
    if (!refreshToken) {
      return new Observable((subscriber) => subscriber.error(new Error('No refresh token')));
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<AuthResponse>('/api/auth/refresh', { refreshToken }).pipe(
        tap((response) => this.store(response)),
        map((response) => response.accessToken),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay(1),
      );
    }
    return this.refreshInFlight$;
  }

  /** Clears the local session, asks the backend to revoke the tokens, and returns to /login. */
  logout(message?: string): void {
    const refreshToken = storage(() => localStorage.getItem(KEYS.refreshToken));
    const accessToken = this.accessToken();
    this.clear();
    if (refreshToken || accessToken) {
      // Token header added manually because the session is already cleared locally.
      const headers: Record<string, string> = accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
      // Best effort: the local session is gone either way.
      this.http.post('/api/auth/logout', { refreshToken }, { headers }).subscribe({ error: () => undefined });
    }
    void this.router.navigateByUrl('/login');
    if (message) {
      this.snack.open(message, 'Close', { duration: 6000 });
    }
  }

  private store(response: AuthResponse): void {
    const session: StoredSession = {
      accessTokenExpiresAt: Date.parse(response.accessTokenExpiresAt),
      refreshTokenExpiresAt: Date.parse(response.refreshTokenExpiresAt),
      user: response.user,
    };
    storage(() => {
      localStorage.setItem(KEYS.accessToken, response.accessToken);
      localStorage.setItem(KEYS.refreshToken, response.refreshToken);
      localStorage.setItem(KEYS.session, JSON.stringify(session));
    });
    this.session.set(session);
  }

  private clear(): void {
    storage(() => {
      localStorage.removeItem(KEYS.accessToken);
      localStorage.removeItem(KEYS.refreshToken);
      localStorage.removeItem(KEYS.session);
    });
    this.session.set(null);
  }
}

function readSession(): StoredSession | null {
  const raw = storage(() => localStorage.getItem(KEYS.session));
  if (!raw) return null;
  try {
    const value = JSON.parse(raw) as StoredSession;
    return typeof value?.refreshTokenExpiresAt === 'number' && value.user?.username ? value : null;
  } catch {
    return null;
  }
}

/** localStorage can throw (private mode, blocked storage); treat that as "nothing stored". */
function storage<T>(action: () => T): T | null {
  try {
    return action();
  } catch {
    return null;
  }
}
