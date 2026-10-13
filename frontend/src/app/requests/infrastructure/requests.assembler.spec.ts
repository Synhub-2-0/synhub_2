import { RequestsAssembler } from './requests.assembler';
import { RequestResource } from './requests.response';
import { CreateRequestCommand } from '../domain/model/create-request.command';

describe('RequestsAssembler', () => {
  const assembler = new RequestsAssembler();
  const resource = {
    id: 3, description: 'Listo', requestType: 'SUBMISSION', requestStatus: 'PENDING',
    task: { id: 7, title: 'Login', description: '', dueDate: '2026-12-01T10:00:00Z', createdAt: '2026-10-01T10:00:00Z', updatedAt: '2026-10-01T10:00:00Z', status: 'COMPLETED' },
  } as RequestResource;

  it('resource → entidad con la tarea anidada convertida', () => {
    const request = assembler.toEntityFromResource(resource);
    expect([request.id, request.requestType, request.requestStatus]).toEqual([3, 'SUBMISSION', 'PENDING']);
    expect(request.task.title).toBe('Login');
    expect(request.task.dueDate).toEqual(new Date('2026-12-01T10:00:00Z'));
  });

  it('respuesta envoltorio, incluso vacía', () => {
    expect(assembler.toEntitiesFromResponse({ requests: [resource] })).toHaveLength(1);
    expect(assembler.toEntitiesFromResponse(undefined as never)).toEqual([]);
  });

  it('comando → request sin el id de la tarea (viaja en la ruta)', () => {
    expect(assembler.toRequestFromCreateCommand(new CreateRequestCommand({ taskId: 7, description: 'Listo', requestType: 'SUBMISSION' })))
      .toEqual({ description: 'Listo', requestType: 'SUBMISSION' });
  });
});
