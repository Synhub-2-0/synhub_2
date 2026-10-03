import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ToastStore } from '../../../application/toast.store';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './toast-container.html',
  styleUrl: './toast-container.css',
})
export class ToastContainer {
  readonly toastStore = inject(ToastStore);
}
