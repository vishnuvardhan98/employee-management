import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Employee, EmployeeService, DEPARTMENTS } from './employee.service';
import { EmployeeTableComponent } from './employee-table.component';
import { EmployeeDialogComponent, DeleteDialogComponent } from './employee-dialog.component';
@Component({selector:'app-employees',imports:[ReactiveFormsModule,MatButtonModule,MatFormFieldModule,MatSelectModule,MatInputModule,EmployeeTableComponent],template:`
<div class="page-heading"><div><span class="eyebrow">PEOPLE OPERATIONS</span><h1>Your people, at a glance.</h1><p class="muted">A home for your team and the details that matter.</p></div><button mat-flat-button (click)="open()">＋ Add employee</button></div>
<section class="stats-grid" aria-label="Employee statistics"><article class="stat-card"><span>Total employees</span><div>{{service.employees().length}}<span class="stat-icon">◫</span></div><small>Your entire team, together</small></article><article class="stat-card"><span>Active employees</span><div>{{active()}}<span class="stat-icon mint">↗</span></div><small><span class="green-dot">●</span> Ready to make things happen</small></article><article class="stat-card"><span>Departments</span><div>{{departmentCount()}}<span class="stat-icon lilac">▦</span></div><small>Different strengths. One team.</small></article><article class="stat-card"><span>On leave</span><div>{{leave()}}<span class="stat-icon peach">☀</span></div><small>Taking a well-deserved pause</small></article></section>
<section class="panel"><div class="panel-heading"><div><h2>Employee directory <span class="count-badge">{{filtered().length}}</span></h2><p class="muted">Manage and keep your team’s information up to date.</p></div></div><div class="filter-bar"><mat-form-field appearance="outline" class="search-field"><mat-label>Search name, email or job title</mat-label><input matInput [formControl]="search">@if(search.value){<button matSuffix mat-button (click)="search.setValue('')" aria-label="Clear search">×</button>}</mat-form-field><mat-form-field appearance="outline"><mat-label>Department</mat-label><mat-select [value]="department()" (selectionChange)="department.set($event.value)"><mat-option value="">All departments</mat-option>@for(d of departments;track d){<mat-option [value]="d">{{d}}</mat-option>}</mat-select></mat-form-field><mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select [value]="status()" (selectionChange)="status.set($event.value)"><mat-option value="">All statuses</mat-option>@for(s of statuses;track s){<mat-option [value]="s">{{s}}</mat-option>}</mat-select></mat-form-field><button mat-button (click)="clear()">Clear filters</button></div><app-employee-table [records]="filtered()" [editable]="true" (edit)="open($event)" (remove)="remove($event)" /></section><p class="table-hint">Tip: select an employee row to edit their details.</p>`})
export class EmployeesComponent {
 service=inject(EmployeeService);private dialog=inject(MatDialog);private snack=inject(MatSnackBar);
 departments=DEPARTMENTS;statuses=['Active','On leave','Inactive'];search=new FormControl('',{nonNullable:true});query=toSignal(this.search.valueChanges.pipe(debounceTime(300),distinctUntilChanged()),{initialValue:''});department=signal('');status=signal('');
 active=computed(()=>this.service.employees().filter(e=>e.status==='Active').length);leave=computed(()=>this.service.employees().filter(e=>e.status==='On leave').length);departmentCount=computed(()=>new Set(this.service.employees().map(e=>e.department)).size);
 filtered=computed(()=>{const q=this.query().toLowerCase().trim();return this.service.employees().filter(e=>(!this.department()||e.department===this.department())&&(!this.status()||e.status===this.status())&&`${e.name} ${e.email} ${e.title}`.toLowerCase().includes(q));});
 clear(){this.search.setValue('');this.department.set('');this.status.set('');}
 open(employee?:Employee){this.dialog.open(EmployeeDialogComponent,{data:employee??null,width:'660px',maxWidth:'95vw'}).afterClosed().subscribe(saved=>{if(saved)this.snack.open(employee?'Employee updated.':'Employee added.','Close',{duration:3000});});}
 remove(employee:Employee){this.dialog.open(DeleteDialogComponent,{data:employee,width:'440px'}).afterClosed().subscribe(confirmed=>{if(confirmed){try{this.service.delete(employee.id);this.snack.open('Employee deleted.','Close',{duration:3000});}catch{this.snack.open('Unable to save. Check your browser storage.','Close',{duration:5000});}}});}
}
