import { CreateTaskCommand } from '../domain/model/create-task.command';
import { TasksAssembler } from './tasks.assembler';
import { TaskResource } from './tasks.response';

describe('TasksAssembler (campos de IA)', () => {
  const assembler = new TasksAssembler();

  it('envía los campos de IA al crear la tarea', () => {
    const command = new CreateTaskCommand('Migrar', 'desc', new Date('2026-12-01T10:00:00Z'), 2, 1, 4, 'DATABASE', 'HIGH', 'UUID, PostgreSQL', true);
    expect(assembler.toCreateRequestFromCommand(command)).toMatchObject({
      difficulty: 4, context: 'DATABASE', urgency: 'HIGH', labels: 'UUID, PostgreSQL', aiAccepted: true,
    });
  });

  it('lee los campos de IA de la respuesta y tolera tareas antiguas sin ellos', () => {
    const base = { id: 1, title: 't', description: 'd', dueDate: '2026-12-01T10:00:00Z', createdAt: '2026-10-01T10:00:00Z', updatedAt: '2026-10-01T10:00:00Z', status: 'IN_PROGRESS' } as TaskResource;
    const withAi = assembler.toEntityFromResource({ ...base, difficulty: 3, context: 'BACKEND', urgency: 'LOW', labels: 'api', aiAccepted: false });
    expect([withAi.difficulty, withAi.context, withAi.urgency, withAi.labels, withAi.aiAccepted]).toEqual([3, 'BACKEND', 'LOW', 'api', false]);
    const legacy = assembler.toEntityFromResource({ ...base, difficulty: null, context: null });
    expect(legacy.difficulty).toBeUndefined();
    expect(legacy.context).toBeUndefined();
  });
});
