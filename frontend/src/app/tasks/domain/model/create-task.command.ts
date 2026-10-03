export class CreateTaskCommand {
  constructor(
    public readonly title: string,
    public readonly description: string,
    public readonly dueDate: Date,
    public readonly userId: number,
    public readonly groupId: number,
    public readonly difficulty?: number,
    public readonly context?: string,
    public readonly urgency?: string,
    public readonly labels?: string,
    public readonly aiAccepted?: boolean,
  ) {}
}
