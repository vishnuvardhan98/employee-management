import { Injectable, NgZone, inject } from '@angular/core';
import { MatSnackBar, MatSnackBarRef, TextOnlySnackBar } from '@angular/material/snack-bar';
import { AuthService } from './auth.service';

const IDLE_LIMIT_MS = 30 * 60 * 1000;
const WARNING_BEFORE_MS = 60 * 1000;
const ACTIVITY_EVENTS = ['pointerdown', 'keydown', 'pointermove', 'scroll', 'wheel'] as const;

/** Signs the user out after 30 minutes without input, with a one-minute warning. */
@Injectable({ providedIn: 'root' })
export class IdleService {
  private readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);
  private readonly zone = inject(NgZone);

  private timer?: ReturnType<typeof setInterval>;
  private lastActivity = Date.now();
  private warning?: MatSnackBarRef<TextOnlySnackBar>;

  private readonly onActivity = () => {
    this.lastActivity = Date.now();
    if (this.warning) {
      this.zone.run(() => this.warning?.dismiss());
      this.warning = undefined;
    }
  };

  start(): void {
    this.stop();
    this.lastActivity = Date.now();
    // Listeners run outside Angular so mouse movement does not trigger change detection.
    this.zone.runOutsideAngular(() => {
      ACTIVITY_EVENTS.forEach((e) => window.addEventListener(e, this.onActivity, { passive: true }));
      this.timer = setInterval(() => this.check(), 1000);
    });
  }

  stop(): void {
    clearInterval(this.timer);
    ACTIVITY_EVENTS.forEach((e) => window.removeEventListener(e, this.onActivity));
    this.warning?.dismiss();
    this.warning = undefined;
  }

  private check(): void {
    const idle = Date.now() - this.lastActivity;
    if (idle >= IDLE_LIMIT_MS) {
      this.zone.run(() => {
        this.stop();
        this.auth.logout('You were signed out after 30 minutes of inactivity.');
      });
    } else if (idle >= IDLE_LIMIT_MS - WARNING_BEFORE_MS && !this.warning) {
      this.zone.run(() => {
        this.warning = this.snack.open(
          'You will be signed out in one minute due to inactivity.',
          'Stay signed in',
          { duration: WARNING_BEFORE_MS },
        );
        this.warning.onAction().subscribe(() => this.onActivity());
      });
    }
  }
}
