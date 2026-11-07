import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { HttpInterceptorFn } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
const SESSION_KEY='peopledesk.demo.session';
@Injectable({providedIn:'root'})
export class AuthService {
 private router=inject(Router);
 private snack=inject(MatSnackBar);
 private timer?:ReturnType<typeof setInterval>;
 private lastActivity=Date.now();
 private warned=false;
 private activity=()=>{this.lastActivity=Date.now();this.warned=false;};
 get valid():boolean {
  try { const session=JSON.parse(localStorage.getItem(SESSION_KEY)||'null');return !!session && session.expires>Date.now(); } catch {return false;}
 }
 login(username:string,password:string,remember:boolean):boolean {
  if(username!=='admin' || password!=='Admin@123') return false;
  localStorage.setItem(SESSION_KEY,JSON.stringify({username,expires:Date.now()+8*60*60*1000}));
  if(remember) localStorage.setItem('peopledesk.username',username);else localStorage.removeItem('peopledesk.username');
  this.startIdle();return true;
 }
 startIdle(){
  this.stopIdle();this.lastActivity=Date.now();this.warned=false;
  ['pointerdown','keydown','pointermove','scroll'].forEach(e=>window.addEventListener(e,this.activity,{passive:true}));
  this.timer=setInterval(()=>{
   const idle=Date.now()-this.lastActivity;
   if(!this.valid || idle>=30*60*1000){this.logout();this.snack.open('Your session ended. Please sign in again.','Close',{duration:6000});}
   else if(idle>=29*60*1000&&!this.warned){this.warned=true;this.snack.open('You will be signed out in one minute. Move your mouse or use the keyboard to stay signed in.','Close',{duration:15000});}
  },1000);
 }
 private stopIdle(){clearInterval(this.timer);['pointerdown','keydown','pointermove','scroll'].forEach(e=>window.removeEventListener(e,this.activity));}
 logout(){this.stopIdle();localStorage.removeItem(SESSION_KEY);void this.router.navigateByUrl('/login');}
}
export const authGuard:CanActivateFn=()=>inject(AuthService).valid || inject(Router).createUrlTree(['/login']);
// Demo authentication never creates a pretend JWT. Replace login with the Spring Boot
// POST /api/auth/login response, then attach its real token to the API origin only.
export const authInterceptor:HttpInterceptorFn=(req,next)=>{
 const token=localStorage.getItem('peopledesk.jwt');
 const api=new URL('http://localhost:8080/api/');
 const target=new URL(req.url,window.location.origin);
 return next(token&&target.origin===api.origin&&target.pathname.startsWith(api.pathname)?req.clone({setHeaders:{Authorization:`Bearer ${token}`}}):req);
};
