import { Component, EventEmitter, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [MatIconModule],
  selector: 'app-no-group-leader',
  styleUrl: './no-group-leader.css',
  templateUrl: './no-group-leader.html',
})
export class NoGroupLeader {
  @Output() createRequested = new EventEmitter<void>();
}
