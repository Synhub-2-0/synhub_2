export class UpdateTaskCommand {
  constructor(
    public readonly requesterId: number,
    public readonly title: string,
    public readonly description: string,
    public readonly dueDate: Date,
    public readonly userId: number,
  ) {}
}
