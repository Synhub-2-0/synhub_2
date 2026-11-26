import { GroupUser } from '../../../../groups/domain/model/group-user.entity';
import { Profile } from '../../../../iam/domain/model/profile.entity';
import { Task } from '../../../../tasks/domain/model/task.entity';
import { MemberWorkloadBreakdown } from './member-workload-breakdown';

const member = (id: number, name: string) =>
  new GroupUser({
    user: new Profile({ id, name, surname: 'Equipo', email: '', imgUrl: '', userId: id }),
    roleInGroup: 'GROUP_MEMBER',
  });

const task = (id: number, assignedTo: number, status: Task['status']) =>
  new Task({
    id,
    title: `Tarea ${id}`,
    description: '',
    dueDate: new Date('2026-12-01T10:00:00Z'),
    createdAt: new Date('2026-10-01T10:00:00Z'),
    updatedAt: new Date('2026-10-01T10:00:00Z'),
    status,
    assignedTo: {
      id: assignedTo,
      username: '',
      name: '',
      surname: '',
      imgUrl: '',
      email: '',
    },
  });

describe('MemberWorkloadBreakdown', () => {
  it('calcula asignación y agrupa estados pendientes, vencidos y terminados', () => {
    const component = new MemberWorkloadBreakdown();
    component.members = [member(2, 'Ana'), member(3, 'Luis')];
    component.tasks = [
      task(1, 2, 'IN_PROGRESS'),
      task(2, 2, 'ON_HOLD'),
      task(3, 2, 'COMPLETED'),
      task(4, 2, 'EXPIRED'),
      task(5, 2, 'DONE'),
    ];
    component.totalTasks = 8;

    component.ngOnChanges();

    expect(component.workloads).toEqual([
      {
        id: 2,
        name: 'Ana Equipo',
        assignedTasks: 5,
        allocationPercent: 62.5,
        expiredTasks: 1,
        inProgressTasks: 3,
        completedTasks: 1,
      },
      {
        id: 3,
        name: 'Luis Equipo',
        assignedTasks: 0,
        allocationPercent: 0,
        expiredTasks: 0,
        inProgressTasks: 0,
        completedTasks: 0,
      },
    ]);
  });

  it('evita porcentajes inválidos cuando el grupo no tiene tareas', () => {
    const component = new MemberWorkloadBreakdown();
    component.members = [member(2, 'Ana')];

    component.ngOnChanges();

    expect(component.workloads[0].allocationPercent).toBe(0);
  });
});
