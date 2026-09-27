import { Component, EventEmitter, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  imports: [MatIconModule],
  selector: 'app-no-group-member',
  styleUrl: './no-group-member.css',
  templateUrl: './no-group-member.html',
})
export class NoGroupMember {
  @Output() joinRequested = new EventEmitter<void>();
}
