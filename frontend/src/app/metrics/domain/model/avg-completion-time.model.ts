export interface AvgCompletionTime {
  type: string;
  /** Días promedio que tardaron en cerrarse las tareas DONE. */
  days: number;
  completedTasks: number;
}
