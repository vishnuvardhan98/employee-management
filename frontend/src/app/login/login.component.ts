import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AuthService } from '../core/auth.service';
import { errorMessage } from '../core/http-utils';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCheckboxModule],
  template: `
    <div class="login-page">
      <section class="login-story">
        <a class="brand"><span class="brand-mark">p.</span> PeopleDesk</a>
        <div>
          <span class="eyebrow">YOUR PEOPLE, CONNECTED</span>
          <h1>A little less admin.<br>A lot more people.</h1>
          <p>One thoughtful workspace to manage your team, keep records organized, and turn everyday data into clarity.</p>
          <div class="story-card"><span class="story-dot"></span> Everything your team needs, in one place.</div>
        </div>
        <small>EMPLOYEE MANAGEMENT WORKSPACE</small>
      </section>

      <main class="login-main">
        <div class="login-box">
          <span class="eyebrow">LET’S GET TO WORK</span>
          <h2>Welcome back</h2>
          <p class="muted">Sign in to your PeopleDesk workspace.</p>

          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field>
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username" maxlength="50">
              <mat-error>
                @if (form.controls.username.hasError('required')) { Username is required. }
                @else { Enter at least 3 characters. }
              </mat-error>
            </mat-form-field>

            <mat-form-field>
              <mat-label>Password</mat-label>
              <input matInput formControlName="password" [type]="showPassword() ? 'text' : 'password'"
                     autocomplete="current-password" maxlength="100">
              <button type="button" mat-button matSuffix (click)="showPassword.set(!showPassword())"
                      [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'">
                {{ showPassword() ? 'Hide' : 'Show' }}
              </button>
              <mat-error>
                @if (form.controls.password.hasError('required')) { Password is required. }
                @else { Enter at least 6 characters. }
              </mat-error>
            </mat-form-field>

            <mat-checkbox formControlName="remember">Remember me</mat-checkbox>

            @if (error()) {
              <p class="error" role="alert">{{ error() }}</p>
            }
            <button class="login-submit" mat-flat-button type="submit" [disabled]="form.invalid || submitting()">
              {{ submitting() ? 'Signing in…' : 'Sign in →' }}
            </button>
          </form>

          <div class="demo-note">
            <strong>Demo accounts</strong>
            <p>Administrator: <code>admin</code> / <code>Admin&#64;123</code></p>
            <p>Read-only user: <code>viewer</code> / <code>Viewer&#64;123</code></p>
            <small>“Remember me” keeps your username and extends the session to 7 days.</small>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly showPassword = signal(false);
  readonly submitting = signal(false);
  readonly error = signal('');

  readonly form = this.fb.group({
    username: [this.auth.rememberedUsername(), [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [!!this.auth.rememberedUsername()],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const { username, password, remember } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set('');
    this.auth.login(username.trim(), password, remember).subscribe({
      next: () => void this.router.navigateByUrl('/employees'),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(errorMessage(err, 'Unable to sign in. Please try again.'));
        this.form.controls.password.reset();
      },
    });
  }
}
