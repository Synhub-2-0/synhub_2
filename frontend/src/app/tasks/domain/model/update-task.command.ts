/** Clasificación de la tarea al editar: reemplaza la guardada; un campo vacío la quita. */
export interface TaskClassificationUpdate {
  difficulty: number | null;
  context: string | null;
  urgency: string | null;
  labels: string | null;
  aiAccepted: boolean | null;
  aiRationale: string | null;
}

export class UpdateTaskCommand {
  constructor(
    public readonly title: string,
    public readonly description: string,
    public readonly dueDate: Date,
    public readonly userId: number,
    public readonly classification?: TaskClassificationUpdate,
  ) {}
}
