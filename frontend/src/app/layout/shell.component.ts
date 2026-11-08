import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { IdleService } from '../core/idle.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatButtonModule],
  template: `
    <div class="workspace">
      <aside class="sidebar">
        <a class="brand" routerLink="/employees"><span class="brand-mark">p.</span> PeopleDesk</a>
        <div class="workspace-label">WORKSPACE</div>
        <nav aria-label="Main navigation">
          <a routerLink="/employees" routerLinkActive="selected"><span aria-hidden="true">◫</span> Employees</a>
          @if (auth.isAdmin()) {
            <a routerLink="/reports" routerLinkActive="selected"><span aria-hidden="true">▥</span> Reports</a>
          }
        </nav>
        <div class="sidebar-bottom">
          <p>Your team. A clearer view.</p>
          <div class="profile">
            <span class="avatar">{{ initials() }}</span>
            <div>
              <strong>{{ auth.user()?.fullName }}</strong>
              <small>{{ roleLabel() }}</small>
            </div>
          </div>
          <button mat-button (click)="signOut()">Sign out ↗</button>
        </div>
      </aside>

      <div class="main-area">
        <header class="topbar">
          <span>Workspace <span class="breadcrumb">/ People operations</span></span>
          <span class="topbar-right">
            <span class="role-chip" [class.admin]="auth.isAdmin()">{{ roleLabel() }}</span>
            <span class="avatar small" [attr.title]="auth.user()?.fullName">{{ initials() }}</span>
            <button class="mobile-signout" mat-button (click)="signOut()">Sign out</button>
          </span>
        </header>
        <main class="page-content"><router-outlet /></main>
        <footer>PeopleDesk <span>Made for the people behind the work.</span></footer>
      </div>
    </div>
  `,
})
export class ShellComponent implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly idle = inject(IdleService);

  readonly roleLabel = computed(() => (this.auth.isAdmin() ? 'Administrator' : 'Read-only user'));
  readonly initials = computed(() =>
    (this.auth.user()?.fullName ?? '')
      .split(/\s+/)
      .filter(Boolean)
      .map((p) => p[0].toUpperCase())
      .slice(0, 2)
      .join(''),
  );

  ngOnInit(): void {
    this.idle.start();
  }

  ngOnDestroy(): void {
    this.idle.stop();
  }

  signOut(): void {
    this.idle.stop();
    this.auth.logout('You have been signed out.');
  }
}
