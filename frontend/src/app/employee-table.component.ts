import { AfterViewInit, Component, EventEmitter, Input, OnChanges, Output, ViewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { Employee } from './employee.service';
@Component({selector:'app-employee-table',imports:[DatePipe,MatTableModule,MatPaginatorModule,MatSortModule,MatButtonModule],template:`
<div class="table-scroll"><table mat-table [dataSource]="source" matSort matSortActive="name" matSortDirection="asc">
<ng-container matColumnDef="name"><th mat-header-cell *matHeaderCellDef mat-sort-header>Employee</th><td mat-cell *matCellDef="let e"><div class="person-cell"><span class="avatar" [class.lilac]="e.id%3===0" [class.peach]="e.id%3===1">{{initials(e.name)}}</span><div><strong>{{e.name}}</strong><small>{{e.email}}</small></div></div></td></ng-container>
<ng-container matColumnDef="department"><th mat-header-cell *matHeaderCellDef mat-sort-header>Department</th><td mat-cell *matCellDef="let e">{{e.department}}</td></ng-container>
<ng-container matColumnDef="title"><th mat-header-cell *matHeaderCellDef mat-sort-header>Job title</th><td mat-cell *matCellDef="let e">{{e.title}}</td></ng-container>
<ng-container matColumnDef="status"><th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th><td mat-cell *matCellDef="let e"><span class="status-pill" [class.leave]="e.status==='On leave'" [class.inactive]="e.status==='Inactive'">● {{e.status}}</span></td></ng-container>
<ng-container matColumnDef="joined"><th mat-header-cell *matHeaderCellDef mat-sort-header>Join date</th><td mat-cell *matCellDef="let e">{{e.joined|date:'MMM d, y'}}</td></ng-container>
<ng-container matColumnDef="actions"><th mat-header-cell *matHeaderCellDef>Actions</th><td mat-cell *matCellDef="let e"><button mat-button (click)="$event.stopPropagation();edit.emit(e)" [attr.aria-label]="'Edit '+e.name">Edit</button><button mat-button class="delete-link" (click)="$event.stopPropagation();remove.emit(e)" [attr.aria-label]="'Delete '+e.name">Delete</button></td></ng-container>
<tr mat-header-row *matHeaderRowDef="columns"></tr><tr mat-row *matRowDef="let row; columns: columns" [class.editable-row]="editable" (click)="editable&&edit.emit(row)" (keydown.enter)="editable&&edit.emit(row)" [attr.tabindex]="editable?0:null" [attr.aria-label]="editable?'Edit '+row.name:null"></tr>
<tr *matNoDataRow><td [attr.colspan]="columns.length" class="empty-state"><strong>No employees found</strong><p>Try a different search or clear your filters.</p></td></tr></table></div>
<mat-paginator [pageSize]="10" [pageSizeOptions]="[10,25,50]" showFirstLastButtons aria-label="Employee pagination" />`})
export class EmployeeTableComponent implements OnChanges,AfterViewInit {
 @Input() records:Employee[]=[];@Input() editable=false;@Output() edit=new EventEmitter<Employee>();@Output() remove=new EventEmitter<Employee>();
 @ViewChild(MatPaginator) paginator!:MatPaginator;@ViewChild(MatSort) sort!:MatSort;
 source=new MatTableDataSource<Employee>();get columns(){return ['name','department','title','status','joined',...(this.editable?['actions']:[])];}
 ngOnChanges(){this.source.data=this.records;this.paginator?.firstPage();}
 ngAfterViewInit(){this.source.paginator=this.paginator;this.source.sort=this.sort;}
 initials(name:string){return name.split(' ').map(n=>n[0]).slice(0,2).join('');}
}
