import { Routes } from '@angular/router';
import { authGuard } from './auth.service';
export const routes: Routes = [
 {path:'login', loadComponent:()=>import('./login.component').then(m=>m.LoginComponent)},
 {path:'', canActivate:[authGuard], loadComponent:()=>import('./shell.component').then(m=>m.ShellComponent), children:[
  {path:'',pathMatch:'full',redirectTo:'employees'},
  {path:'employees',loadComponent:()=>import('./employees.component').then(m=>m.EmployeesComponent)},
  {path:'reports',loadComponent:()=>import('./reports.component').then(m=>m.ReportsComponent)}
 ]},
 {path:'**',redirectTo:'employees'}
];
