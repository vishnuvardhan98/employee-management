import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Employee } from '../core/api.models';

@Component({
  selector: 'app-delete-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Delete employee?</h2>
    <mat-dialog-content>
      <p>This will permanently remove <strong>{{ employee.name }}</strong> ({{ employee.email }}). This cannot be undone.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close cdkFocusInitial>Cancel</button>
      <button mat-flat-button class="danger-button" [mat-dialog-close]="true">Delete employee</button>
    </mat-dialog-actions>
  `,
})
export class DeleteDialogComponent {
  readonly employee = inject<Employee>(MAT_DIALOG_DATA);
}
