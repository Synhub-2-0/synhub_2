import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TasksStore } from '../../../application/tasks.store';

@Component({
  selector: 'app-task-details',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './task-details.html',
  styleUrl: './task-details.css',
})
export class TaskDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  readonly tasksStore = inject(TasksStore);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.tasksStore.loadTaskById(id);
  }

  goBack(): void { this.location.back(); }
}
