import { Injectable, signal } from '@angular/core';
export interface Employee {id:number;name:string;email:string;phone:string;department:string;title:string;status:'Active'|'On leave'|'Inactive';joined:string;}
export const DEPARTMENTS=['Engineering','Design','People','Marketing','Finance'];
const STORAGE='peopledesk.employees.v1';
function seed():Employee[]{
 const people=[['Olivia Chen','Engineering','Frontend Developer'],['James Wilson','Design','Product Designer'],['Ava Patel','People','People Partner'],['Noah Williams','Marketing','Content Strategist'],['Sophia Garcia','Finance','Financial Analyst'],['Liam Anderson','Engineering','Backend Developer'],['Isabella Thomas','Design','UX Researcher'],['Ethan Martinez','Engineering','QA Engineer'],['Mia Robinson','Marketing','Marketing Manager'],['Lucas Taylor','People','Recruiter'],['Amelia Moore','Finance','Accountant'],['Benjamin Lee','Engineering','DevOps Engineer'],['Charlotte Davis','Design','Design Lead'],['Henry Jackson','Marketing','SEO Specialist'],['Harper White','People','HR Coordinator'],['Alexander Harris','Engineering','Engineering Manager'],['Evelyn Clark','Finance','Finance Manager'],['Daniel Lewis','Design','Visual Designer']];
 const now=new Date();const month=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}`;
 return people.map((p,i)=>({id:i+1,name:p[0],email:p[0].toLowerCase().replace(' ','.')+'@example.com',phone:`202-555-${String(100+i).padStart(4,'0')}`,department:p[1],title:p[2],status:i===3||i===9?'On leave':i===14?'Inactive':'Active',joined:i<6?`${month}-${String(Math.min(i+1,now.getDate())).padStart(2,'0')}`:`${now.getFullYear()-1}-${String(i%12+1).padStart(2,'0')}-12`}));
}
@Injectable({providedIn:'root'})
export class EmployeeService {
 readonly employees=signal<Employee[]>(this.load());
 private load():Employee[]{try{const raw=localStorage.getItem(STORAGE);if(raw){const value=JSON.parse(raw);if(Array.isArray(value)&&value.every(e=>typeof e.id==='number'&&typeof e.name==='string'&&typeof e.email==='string'&&typeof e.joined==='string'&&DEPARTMENTS.includes(e.department)&&['Active','On leave','Inactive'].includes(e.status)))return value;}}catch{}return seed();}
 save(value:Omit<Employee,'id'>,id?:number){
  const records=this.employees();
  if(records.some(e=>e.id!==id&&e.email.toLowerCase()===value.email.toLowerCase()))throw new Error('An employee with this email already exists.');
  const employee={...value,id:id??Math.max(0,...records.map(e=>e.id))+1};
  this.commit(id?records.map(e=>e.id===id?employee:e):[employee,...records]);
 }
 delete(id:number){this.commit(this.employees().filter(e=>e.id!==id));}
 private commit(records:Employee[]){localStorage.setItem(STORAGE,JSON.stringify(records));this.employees.set(records);}
}
