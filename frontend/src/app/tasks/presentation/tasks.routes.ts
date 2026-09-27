import { Routes } from '@angular/router';

const leaderTasks = () => import('./views/leader-tasks/leader-tasks').then(m => m.LeaderTasks);
const memberTasks = () => import('./views/member-tasks/member-tasks').then(m => m.MemberTasks);
const taskForm = () => import('./views/task-form/task-form').then(m => m.TaskForm);
const taskDetails = () => import('./views/task-details/task-details').then(m => m.TaskDetails);

export const tasksRoutes: Routes = [
  { path: 'leader', loadComponent: leaderTasks },
  { path: 'member', loadComponent: memberTasks },
  { path: 'create', loadComponent: taskForm },
  { path: ':id/edit', loadComponent: taskForm },
  { path: ':id', loadComponent: taskDetails },
  { path: '', redirectTo: 'member', pathMatch: 'full' },
];
