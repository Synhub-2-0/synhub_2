import { TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LeaderTasks } from './leader-tasks/leader-tasks';
import { MemberTasks } from './member-tasks/member-tasks';
import { TaskDetails } from './task-details/task-details';
import { TaskForm } from './task-form/task-form';
import { TaskList } from '../components/task-list/task-list';
import { TasksApi } from '../../infrastructure/tasks.api';
import { GroupsApi } from '../../../groups/infrastructure/groups.api';
import { IamApi } from '../../../iam/infrastructure/iam.api';
import { AiApi } from '../../../ai/infrastructure/ai.api';
import { RequestsApi } from '../../../requests/infrastructure/requests.api';
import { TasksStore } from '../../application/tasks.store';
import { CreateTaskCommand } from '../../domain/model/create-task.command';
import { TaskClassification } from '../../../ai/domain/model/task-classification.entity';

/** Doble de una API: cualquier método devuelve un observable vacío. */
const apiStub = () => new Proxy({} as Record<string, ReturnType<typeof vi.fn>>, {
  get: (target, prop: string) => (target[prop] ??= vi.fn(() => of([]))),
});

describe('Vistas de tasks', () => {
  let aiApi: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(async () => {
    localStorage.clear();
    aiApi = apiStub();
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '**', children: [] }]), // save() navega a /tasks/leader
        { provide: TasksApi, useValue: apiStub() },
        { provide: GroupsApi, useValue: apiStub() },
        { provide: IamApi, useValue: apiStub() },
        { provide: RequestsApi, useValue: apiStub() },
        { provide: AiApi, useValue: aiApi },
      ],
    }).compileComponents();
  });

  it.each<[string, Type<unknown>]>([
    ['LeaderTasks', LeaderTasks],
    ['MemberTasks', MemberTasks],
    ['TaskDetails', TaskDetails],
    ['TaskForm', TaskForm],
    ['TaskList', TaskList],
  ])('%s se crea', async (_, component) => {
    const fixture = TestBed.createComponent(component);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
  });

  describe('TaskForm: medición de aiAccepted', () => {
    const suggestion: TaskClassification = { labels: ['login', 'auth'], urgency: 'HIGH', difficulty: 4, context: 'BACKEND', rationale: 'r' };

    const saveWith = async (useAi: boolean, edit?: (form: TaskForm) => void) => {
      const fixture = TestBed.createComponent(TaskForm);
      const form = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      const addTask = vi.spyOn(TestBed.inject(TasksStore), 'addTask').mockImplementation(() => undefined);
      Object.assign(form, { title: 'Login', dueDateTime: '2026-12-01T10:00', groupId: 10, userId: 5 });
      if (useAi) {
        aiApi['classifyTask'].mockReturnValue(of(suggestion));
        form.suggest();
        fixture.detectChanges();
        await fixture.whenStable();
      }
      edit?.(form);
      form.save();
      return addTask.mock.calls[0][0] as CreateTaskCommand;
    };

    it('sin IA: aiAccepted es undefined', async () => {
      expect((await saveWith(false)).aiAccepted).toBeUndefined();
    });

    it('sugerencia guardada tal cual: true, con los campos sugeridos', async () => {
      const command = await saveWith(true);
      expect(command.aiAccepted).toBe(true);
      expect([command.difficulty, command.context, command.urgency, command.labels]).toEqual([4, 'BACKEND', 'HIGH', 'login, auth']);
    });

    it('sugerencia editada: false', async () => {
      expect((await saveWith(true, (form) => (form.difficulty = 2))).aiAccepted).toBe(false);
    });
  });
});
