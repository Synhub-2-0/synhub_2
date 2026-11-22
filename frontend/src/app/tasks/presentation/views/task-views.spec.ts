import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { Type, signal } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
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
import { GroupsStore } from '../../../groups/application/groups.store';
import { CreateTaskCommand } from '../../domain/model/create-task.command';
import { UpdateTaskCommand } from '../../domain/model/update-task.command';
import { Task } from '../../domain/model/task.entity';
import { IamStore } from '../../../iam/application/iam.store';
import { TaskClassification } from '../../../ai/domain/model/task-classification.entity';

/** Doble de una API: cualquier metodo devuelve un observable vacío. */
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

  describe('TaskDetails: helpers de presentación', () => {
    it('labelsOf separa etiquetas y descarta vacíos', () => {
      const fixture = TestBed.createComponent(TaskDetails);
      const component = fixture.componentInstance;
      expect(component.labelsOf('login, auth, ')).toEqual(['login', 'auth']);
      expect(component.labelsOf('')).toEqual([]);
      expect(component.labelsOf(undefined)).toEqual([]);
    });

    it('isAssignee: solo el integrante asignado ve las acciones de su tarea', () => {
      const component = TestBed.createComponent(TaskDetails).componentInstance;
      const iam = TestBed.inject(IamStore) as unknown as { currentUserId: () => number | null };
      const assigned = new Task({
        id: 7, title: 'T', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS',
        assignedTo: { id: 5, username: 'ana', name: 'Ana', surname: 'T', imgUrl: '', email: '' },
      });
      vi.spyOn(iam, 'currentUserId').mockReturnValue(5);
      expect(component.isAssignee(assigned)).toBe(true);
      vi.spyOn(iam, 'currentUserId').mockReturnValue(9);
      expect(component.isAssignee(assigned)).toBe(false);
      vi.spyOn(iam, 'currentUserId').mockReturnValue(null);
      expect(component.isAssignee(assigned)).toBe(false);
    });

    it('openGroup lleva a la pantalla del grupo de la tarea', () => {
      const component = TestBed.createComponent(TaskDetails).componentInstance;
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      component.openGroup(new Task({
        id: 7, title: 'T', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS',
        group: { id: 3, name: 'Diseño', imgUrl: '', description: '', code: 'ABC', memberCount: 1, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
      }));
      expect(navigate).toHaveBeenCalledWith(['/groups', 3]);
    });

    it('labelFor devuelve el texto en español para cada estado', () => {
      const component = TestBed.createComponent(TaskDetails).componentInstance;
      expect(component.labelFor('IN_PROGRESS')).toBe('En progreso');
      expect(component.labelFor('ON_HOLD')).toBe('En espera');
      expect(component.labelFor('COMPLETED')).toBe('Completada');
      expect(component.labelFor('DONE')).toBe('Terminada');
      expect(component.labelFor('EXPIRED')).toBe('Vencida');
    });

    it('ngOnInit carga la tarea cuando la ruta trae un id', async () => {
      TestBed.resetTestingModule();
      const loadTaskById = vi.fn();
      await TestBed.configureTestingModule({
        imports: [TaskDetails],
        providers: [
          provideRouter([]),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '42' }) } } },
          { provide: TasksApi, useValue: apiStub() },
          { provide: GroupsApi, useValue: apiStub() },
          { provide: IamApi, useValue: apiStub() },
          { provide: RequestsApi, useValue: apiStub() },
          { provide: AiApi, useValue: apiStub() },
          { provide: TasksStore, useValue: { loadTaskById, loading: signal(false), selectedTask: signal(null), error: signal(null) } },
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(TaskDetails);
      fixture.detectChanges();
      await fixture.whenStable();
      expect(loadTaskById).toHaveBeenCalledWith(42);
    });

    it('goBack delega a Location.back()', () => {
      const back = vi.fn();
      TestBed.overrideProvider(Location, { useValue: { back } });
      const component = TestBed.createComponent(TaskDetails).componentInstance;
      component.goBack();
      expect(back).toHaveBeenCalled();
    });
  });

  describe('LeaderTasks: selección de grupo y filtros', () => {
    const makeGroup = (id: number) => ({ id, name: `G${id}`, imgUrl: '', description: '', code: 'ABC123XYZ', memberCount: 1 });

    it('en un grupo bloqueado el líder ve el aviso y no puede crear tareas', async () => {
      TestBed.resetTestingModule();
      const leaderGroups = signal([{ ...makeGroup(10), locked: true }, makeGroup(20)]);
      await TestBed.configureTestingModule({
        imports: [LeaderTasks],
        providers: [
          provideRouter([]),
          { provide: TasksApi, useValue: apiStub() },
          { provide: GroupsApi, useValue: apiStub() },
          { provide: IamApi, useValue: apiStub() },
          { provide: AiApi, useValue: apiStub() },
          { provide: RequestsApi, useValue: apiStub() },
          { provide: TasksStore, useValue: { loadTasksByGroup: vi.fn(), setStatusFilter: vi.fn(), tasks: signal([]), filteredTasks: signal([]), loading: signal(false), error: signal(null), statusFilter: signal('ALL') } },
          { provide: GroupsStore, useValue: { leaderGroups, memberGroups: signal([]), groups: signal([]), loading: signal(false), loadGroups: vi.fn() } },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(LeaderTasks);
      fixture.detectChanges();
      await fixture.whenStable();
      const component = fixture.componentInstance;
      const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

      expect(component.selectedGroupLocked()).toBe(true);
      expect(text()).toContain('Grupo bloqueado');
      expect(text()).not.toContain('Crear Tarea');

      component.selectGroup(20);
      fixture.detectChanges();
      expect(component.selectedGroupLocked()).toBe(false);
      expect(text()).toContain('Crear Tarea');
    });

    it('selecciona el primer grupo al iniciar y pasa el filtro al store', async () => {
      TestBed.resetTestingModule();
      const loadTasksByGroup = vi.fn();
      const setStatusFilter = vi.fn();
      const leaderGroups = signal([makeGroup(10), makeGroup(20)]);
      await TestBed.configureTestingModule({
        imports: [LeaderTasks],
        providers: [
          provideRouter([]),
          { provide: TasksApi, useValue: apiStub() },
          { provide: GroupsApi, useValue: apiStub() },
          { provide: IamApi, useValue: apiStub() },
          { provide: AiApi, useValue: apiStub() },
          { provide: RequestsApi, useValue: apiStub() },
          {
            provide: TasksStore,
            useValue: { loadTasksByGroup, setStatusFilter, tasks: signal([]), filteredTasks: signal([]), loading: signal(false), error: signal(null), statusFilter: signal('ALL') },
          },
          {
            provide: GroupsStore,
            useValue: { leaderGroups, memberGroups: signal([]), groups: signal([makeGroup(10), makeGroup(20)]), loading: signal(false), loadGroups: vi.fn() },
          },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        ],
      }).compileComponents();

      const fixture = TestBed.createComponent(LeaderTasks);
      fixture.detectChanges();
      await fixture.whenStable();
      const component = fixture.componentInstance;

      expect(component.selectedGroupId()).toBe(10);
      expect(loadTasksByGroup).toHaveBeenCalledWith(10);

      component.selectGroup(20);
      expect(component.selectedGroupId()).toBe(20);
      expect(loadTasksByGroup).toHaveBeenCalledWith(20);

      component.setFilter('DONE');
      expect(setStatusFilter).toHaveBeenCalledWith('DONE');
    });

    it('labelFor devuelve la etiqueta correcta para cada estado', () => {
      const fixture = TestBed.createComponent(LeaderTasks);
      const component = fixture.componentInstance;
      expect(component.labelFor('ALL')).toBe('Todas');
      expect(component.labelFor('IN_PROGRESS')).toBe('En progreso');
      expect(component.labelFor('DONE')).toBe('Terminadas');
      expect(component.labelFor('EXPIRED')).toBe('Vencidas');
    });
  });
});
