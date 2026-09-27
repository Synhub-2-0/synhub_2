export class CreateTaskCommand {
  constructor(
    public readonly title: string,
    public readonly description: string,
    public readonly dueDate: Date,
    public readonly userId: number,
    public readonly groupId: number,
  ) {}
}
