import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { DEPARTMENTS, Employee, EmployeeService } from './employee.service';
@Component({selector:'app-employee-dialog',imports:[ReactiveFormsModule,MatDialogModule,MatFormFieldModule,MatInputModule,MatSelectModule,MatButtonModule],template:`
<h2 mat-dialog-title>{{data?'Edit employee':'Add employee'}}</h2><form [formGroup]="form" (ngSubmit)="save()"><mat-dialog-content><p class="muted">Keep your team’s details up to date.</p><div class="form-grid">
<mat-form-field appearance="outline"><mat-label>Full name</mat-label><input matInput formControlName="name" autocomplete="name"><mat-error>Name must be 2–80 characters.</mat-error></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Work email</mat-label><input matInput formControlName="email" type="email" autocomplete="email"><mat-error>Enter a valid email.</mat-error></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Phone</mat-label><input matInput formControlName="phone" autocomplete="tel"><mat-error>Enter a valid phone (7–20 characters).</mat-error></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Job title</mat-label><input matInput formControlName="title"><mat-error>Job title is required (max 80 characters).</mat-error></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Department</mat-label><mat-select formControlName="department">@for(d of departments;track d){<mat-option [value]="d">{{d}}</mat-option>}</mat-select></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select formControlName="status">@for(s of statuses;track s){<mat-option [value]="s">{{s}}</mat-option>}</mat-select></mat-form-field>
<mat-form-field appearance="outline"><mat-label>Join date</mat-label><input matInput type="date" formControlName="joined"><mat-error>Join date is required.</mat-error></mat-form-field></div>@if(error){<p class="error" role="alert">{{error}}</p>}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button type="button" mat-dialog-close>Cancel</button><button mat-flat-button type="submit" [disabled]="form.invalid">{{data?'Save changes':'Add employee'}}</button></mat-dialog-actions></form>`})
export class EmployeeDialogComponent {
 data=inject<Employee|null>(MAT_DIALOG_DATA);private ref=inject(MatDialogRef<EmployeeDialogComponent>);private service=inject(EmployeeService);private fb=inject(NonNullableFormBuilder);
 departments=DEPARTMENTS;statuses:Employee['status'][]=['Active','On leave','Inactive'];error='';
 form=this.fb.group({name:[this.data?.name??'',[Validators.required,Validators.minLength(2),Validators.maxLength(80),Validators.pattern(/.*\S.*/)]],email:[this.data?.email??'',[Validators.required,Validators.email,Validators.maxLength(254)]],phone:[this.data?.phone??'',[Validators.required,Validators.pattern(/^[+0-9() .-]{7,20}$/)]],title:[this.data?.title??'',[Validators.required,Validators.maxLength(80),Validators.pattern(/.*\S.*/)]],department:[this.data?.department??'Engineering',Validators.required],status:[this.data?.status??'Active' as Employee['status'],Validators.required],joined:[this.data?.joined??new Date().toLocaleDateString('en-CA'),Validators.required]});
 save(){if(this.form.invalid)return;try{const v=this.form.getRawValue();this.service.save({...v,name:v.name.trim(),email:v.email.trim(),title:v.title.trim(),phone:v.phone.trim()},this.data?.id);this.ref.close(true);}catch(e){this.error=e instanceof Error?e.message:'Unable to save the record.';}}
}
@Component({selector:'app-delete-dialog',imports:[MatDialogModule,MatButtonModule],template:`<h2 mat-dialog-title>Delete employee?</h2><mat-dialog-content><p>This will permanently remove <strong>{{data.name}}</strong> from this browser’s demo records.</p></mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cancel</button><button mat-flat-button class="danger-button" [mat-dialog-close]="true">Delete employee</button></mat-dialog-actions>`})
export class DeleteDialogComponent {data=inject<Employee>(MAT_DIALOG_DATA);}
