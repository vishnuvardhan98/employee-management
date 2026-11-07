import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AuthService } from './auth.service';
@Component({selector:'app-login',imports:[ReactiveFormsModule,MatFormFieldModule,MatInputModule,MatButtonModule,MatCheckboxModule],template:`
<div class="login-page"><section class="login-story"><a class="brand"><span class="brand-mark">p.</span> PeopleDesk</a><div><span class="eyebrow">YOUR PEOPLE, CONNECTED</span><h1>A little less admin.<br>A lot more people.</h1><p>One thoughtful workspace to manage your team, keep records organized, and turn everyday data into clarity.</p><div class="story-card"><span class="story-dot"></span> Everything your team needs, in one place.</div></div><small>EMPLOYEE MANAGEMENT WORKSPACE</small></section>
<main class="login-main"><div class="login-box"><span class="eyebrow">LET’S GET TO WORK</span><h2>Welcome back</h2><p class="muted">Sign in to your PeopleDesk workspace.</p>
<form [formGroup]="form" (ngSubmit)="submit()">
<mat-form-field appearance="outline"><mat-label>Username</mat-label><input matInput formControlName="username" autocomplete="username"><mat-error>Enter at least 3 characters.</mat-error></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Password</mat-label><input matInput formControlName="password" [type]="show?'text':'password'" autocomplete="current-password"><button type="button" mat-button matSuffix (click)="show=!show" [attr.aria-label]="show?'Hide password':'Show password'">{{show?'Hide':'Show'}}</button><mat-error>Enter at least 6 characters.</mat-error></mat-form-field>
<mat-checkbox formControlName="remember">Remember my username</mat-checkbox>
@if(error){<p class="error" role="alert">{{error}}</p>}
<button class="login-submit" mat-flat-button type="submit" [disabled]="form.invalid">Sign in →</button>
</form><div class="demo-note"><strong>Frontend demo</strong><p>Username: <code>admin</code> · Password: <code>Admin&#64;123</code></p><small>Sample records stay in this browser. Secure authentication will be connected with the backend.</small></div></div></main></div>`})
export class LoginComponent {
 private fb=inject(NonNullableFormBuilder);private auth=inject(AuthService);private router=inject(Router);
 show=false;error='';
 form=this.fb.group({username:[localStorage.getItem('peopledesk.username')||'',[Validators.required,Validators.minLength(3)]],password:['',[Validators.required,Validators.minLength(6)]],remember:[!!localStorage.getItem('peopledesk.username')]});
 submit(){if(this.form.invalid)return;const v=this.form.getRawValue();if(this.auth.login(v.username,v.password,v.remember))void this.router.navigateByUrl('/employees');else this.error='Username or password is incorrect.';}
}
