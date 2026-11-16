import { TestBed } from '@angular/core/testing';
import { Type, signal } from '@angular/core';
import { ActivatedRoute, provideRouter } from '@angular/router';
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
import { UpdateTaskCommand } from '../../domain/model/update-task.command';
import { Task } from '../../domain/model/task.entity';
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
      expect(command.aiRationale).toBe('r');
    });

    it('sugerencia editada: false', async () => {
      expect((await saveWith(true, (form) => (form.difficulty = 2))).aiAccepted).toBe(false);
    });

    it('clasificación a mano, sin IA: se guarda y aiAccepted es undefined', async () => {
      const command = await saveWith(false, (form) => Object.assign(form, { difficulty: 2, context: 'QA' }));
      expect([command.difficulty, command.context, command.aiAccepted, command.aiRationale]).toEqual([2, 'QA', undefined, undefined]);
    });

    it('sugerencia quitada: la tarea se crea sin clasificación', async () => {
      const command = await saveWith(true, (form) => form.removeClassification());
      expect([command.difficulty, command.context, command.urgency, command.labels, command.aiAccepted])
        .toEqual([undefined, undefined, undefined, undefined, undefined]);
    });
  });

  describe('TaskForm: editar la clasificación de una tarea', () => {
    const suggestion: TaskClassification = { labels: ['infra'], urgency: 'LOW', difficulty: 5, context: 'CLOUD', rationale: 'r' };
    const saved = (aiAccepted: boolean | null) => new Task({
      id: 7, title: 'Login', description: 'd', dueDate: new Date('2026-12-01T10:00:00Z'), createdAt: new Date(), updatedAt: new Date(),
      status: 'IN_PROGRESS', group: { id: 10 } as Task['group'], assignedTo: { id: 5 } as Task['assignedTo'],
      difficulty: 4, context: 'BACKEND', urgency: 'HIGH', labels: 'login, auth', aiAccepted,
      aiRationale: aiAccepted == null ? null : 'guardada',
    });

    const editWith = async (task: Task, edit?: (form: TaskForm, fixture: { detectChanges(): void; whenStable(): Promise<unknown> }) => Promise<void> | void) => {
      const tasksStore = TestBed.inject(TasksStore);
      vi.spyOn(tasksStore, 'getTaskById').mockReturnValue(signal(task));
      const updateTask = vi.spyOn(tasksStore, 'updateTask').mockImplementation(() => undefined);
      vi.spyOn(TestBed.inject(ActivatedRoute).snapshot.paramMap, 'get').mockReturnValue('7');
      const fixture = TestBed.createComponent(TaskForm);
      const form = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      await edit?.(form, fixture);
      form.save();
      return (updateTask.mock.calls[0][1] as UpdateTaskCommand).classification;
    };

    it('carga la clasificación guardada y, sin cambios, la conserva', async () => {
      expect(await editWith(saved(true))).toEqual({ difficulty: 4, context: 'BACKEND', urgency: 'HIGH', labels: 'login, auth', aiAccepted: true, aiRationale: 'guardada' });
    });

    it('editar a mano una sugerencia aceptada la marca como editada', async () => {
      const classification = await editWith(saved(true), (form) => { form.urgency = 'LOW'; });
      expect(classification).toMatchObject({ urgency: 'LOW', aiAccepted: false, aiRationale: 'guardada' });
    });

    it('editar una clasificación hecha a mano no la atribuye a la IA', async () => {
      expect(await editWith(saved(null), (form) => { form.difficulty = 1; })).toMatchObject({ aiAccepted: null, aiRationale: null });
    });

    it('se puede rehacer con la IA: reemplaza los campos y vuelve a medir', async () => {
      const classification = await editWith(saved(false), async (form, fixture) => {
        aiApi['classifyTask'].mockReturnValue(of(suggestion));
        form.suggest();
        fixture.detectChanges();
        await fixture.whenStable();
      });
      expect(classification).toEqual({ difficulty: 5, context: 'CLOUD', urgency: 'LOW', labels: 'infra', aiAccepted: true, aiRationale: 'r' });
    });

    it('se puede quitar la clasificación', async () => {
      expect(await editWith(saved(true), (form) => form.removeClassification()))
        .toEqual({ difficulty: null, context: null, urgency: null, labels: null, aiAccepted: null, aiRationale: null });
    });
  });
});
