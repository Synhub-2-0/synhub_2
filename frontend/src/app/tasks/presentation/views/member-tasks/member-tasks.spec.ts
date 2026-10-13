import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { MemberTasks } from './member-tasks';
import { TasksStore } from '../../../application/tasks.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { Group } from '../../../../groups/domain/model/group.entity';
import { Task, TaskGroup } from '../../../domain/model/task.entity';

const group = (id: number) => new Group({ id, name: `G${id}`, imgUrl: '', description: '', code: 'ABC123XYZ', memberCount: 2 });
const task = (id: number, groupId: number, assigneeId = 5) => new Task({
  id, title: `T${id}`, description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS',
  group: { id: groupId } as TaskGroup,
  assignedTo: { id: assigneeId, username: 'u', name: 'U', surname: 'T', imgUrl: '', email: '' },
});

describe('MemberTasks (AUDIT PERF-03)', () => {
  let fixture: ComponentFixture<MemberTasks>;
  let component: MemberTasks;
  let loadMyTasks: ReturnType<typeof vi.fn>;
  let tasks: ReturnType<typeof signal<Task[]>>;

  beforeEach(async () => {
    loadMyTasks = vi.fn();
    tasks = signal([task(1, 10), task(2, 20), task(3, 10, 99)]);
    await TestBed.configureTestingModule({
      imports: [MemberTasks],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        {
          provide: TasksStore,
          useValue: {
            loadMyTasks, filteredTasks: tasks, assignedGroupIds: signal([20]), loading: signal(false),
            statusFilter: signal('ALL'), setStatusFilter: vi.fn(), updateTaskStatus: vi.fn(),
          },
        },
        {
          provide: GroupsStore,
          useValue: { groups: signal([group(10), group(20)]), memberGroups: signal([group(10)]), leaderGroups: signal([group(20)]), loadGroups: vi.fn() },
        },
        { provide: IamStore, useValue: { currentUserId: signal(5), isSignedIn: signal(false) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MemberTasks);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('pide mis tareas una sola vez y arranca con todos los grupos', () => {
    expect(loadMyTasks).toHaveBeenCalledTimes(1);
    // Grupo 10 (miembro) y grupo 20 (lo lidera, pero tengo una tarea asignada).
    expect(component.availableGroups().map((g) => g.id)).toEqual([10, 20]);
    expect(component.selectedGroupId()).toBe(component.ALL_GROUPS);
    expect(component.myTasks().map((t) => t.id)).toEqual([1, 2]);
  });

  it('elegir un grupo filtra en el cliente, sin otra petición', () => {
    component.selectGroup(20);
    expect(component.myTasks().map((t) => t.id)).toEqual([2]);
    component.selectAllGroups();
    expect(component.myTasks().map((t) => t.id)).toEqual([1, 2]);
    expect(loadMyTasks).toHaveBeenCalledTimes(1);
  });
});
