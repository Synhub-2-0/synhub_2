import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { WritableSignal, signal } from '@angular/core';
import { Group } from '../../../domain/model/group.entity';
import { GroupDetails } from './group-details';
import { GroupsStore } from '../../../application/groups.store';

describe('GroupDetails', () => {
  let component: GroupDetails;
  let fixture: ComponentFixture<GroupDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupDetails],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  /** Marca al usuario como líder del grupo dado (las métricas del grupo solo se refrescan para el líder). */
  function asLeader(groupId: number): void {
    vi.spyOn(TestBed.inject(GroupsStore), 'leaderGroups').mockImplementation(signal([{ id: groupId }] as never[]) as never);
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el rol sale de los grupos que lidera el usuario, no de la URL', () => {
    const store = TestBed.inject(GroupsStore);
    const led = signal([{ id: 10 }] as never[]);
    vi.spyOn(store, 'leaderGroups').mockImplementation(led as never);
    component.groupId.set(10);
    expect(component.isLeaderView()).toBe(true);
    component.groupId.set(11);
    expect(component.isLeaderView()).toBe(false);
  });

  it('un grupo bloqueado es de solo lectura: el líder no administra pero sigue viéndolo', () => {
    const store = TestBed.inject(GroupsStore);
    const led = signal([{ id: 10 }] as never[]);
    vi.spyOn(store, 'leaderGroups').mockImplementation(led as never);
    const groups = (store as unknown as { groupsSignal: WritableSignal<Group[]> }).groupsSignal;
    const withLock = (locked: boolean) => new Group({ id: 10, name: 'G', imgUrl: '', description: '', code: 'C', memberCount: 1, locked });
    groups.set([withLock(true)]);
    component.groupId.set(10);

    expect(component.isLeaderView()).toBe(true);
    expect(component.isLocked()).toBe(true);
    expect(component.canManage()).toBe(false);

    groups.set([withLock(false)]);
    expect(component.canManage()).toBe(true);
  });

  it('un integrante nunca administra, esté o no bloqueado el grupo', () => {
    component.groupId.set(10);
    expect(component.canManage()).toBe(false);
  });

  it('activar el grupo bloqueado delega al store y refresca los cupos', () => {
    const store = TestBed.inject(GroupsStore);
    const activate = vi.spyOn(store, 'activateGroup').mockImplementation(() => undefined);
    component.groupId.set(10);

    component.activateGroup();

    expect(activate).toHaveBeenCalledWith(10, expect.any(Function));
  });

  it('eliminar el grupo pide confirmación, lo borra y vuelve a Grupos Liderados', () => {
    const store = TestBed.inject(GroupsStore);
    const remove = vi.spyOn(store, 'deleteGroup').mockImplementation(() => undefined);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.groupId.set(10);

    component.askDelete();
    expect(component.confirmingDelete()).toBe(true);
    component.cancelDelete();
    expect(component.confirmingDelete()).toBe(false);
    expect(remove).not.toHaveBeenCalled();

    component.askDelete();
    component.confirmDelete();
    expect(remove).toHaveBeenCalledWith(10, expect.any(Function));
    expect(component.confirmingDelete()).toBe(false);

    remove.mock.calls[0][1]?.();
    expect(navigate).toHaveBeenCalledWith(['/groups/leader']);
  });

  it('el líder puede eliminar su grupo aunque esté bloqueado; un integrante no', () => {
    const store = TestBed.inject(GroupsStore);
    const led = signal([{ id: 10 }] as never[]);
    vi.spyOn(store, 'leaderGroups').mockImplementation(led as never);
    component.groupId.set(10);
    expect(component.canDelete()).toBe(true);
    component.groupId.set(11);
    expect(component.canDelete()).toBe(false);
  });

  it('el botón de tareas lleva a Gestión de tareas del grupo', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.groupId.set(10);
    component.goToTasks();
    expect(navigate).toHaveBeenCalledWith(['/tasks/member'], { queryParams: { groupId: 10 } });
  });

  it('HUM-05: quitar a un integrante pide confirmación', () => {
    const store = TestBed.inject(GroupsStore);
    const remove = vi.spyOn(store, 'removeMember').mockImplementation(() => undefined);
    const loadTasks = vi.spyOn(component.tasksStore, 'loadTasksByGroup').mockImplementation(() => undefined);
    const loadDistribution = vi.spyOn(component.metricsStore, 'loadGroupDistribution').mockImplementation(() => undefined);
    asLeader(10);
    component.groupId.set(10);

    component.askRemoval(2);
    expect(component.pendingRemoval()).toBe(2);
    component.cancelRemoval();
    expect(remove).not.toHaveBeenCalled();

    component.askRemoval(2);
    component.confirmRemoval();
    expect(remove).toHaveBeenCalledWith(10, 2, expect.any(Function));
    expect(component.pendingRemoval()).toBeNull();
    remove.mock.calls[0][2]?.();
    expect(loadTasks).toHaveBeenCalledWith(10);
    expect(loadDistribution).toHaveBeenCalledWith(10);
  });

  it('HUM-05: salir del grupo', () => {
    const store = TestBed.inject(GroupsStore);
    const leave = vi.spyOn(store, 'leaveGroup').mockImplementation(() => undefined);
    component.groupId.set(10);
    component.askRemoval('self');
    component.confirmRemoval();
    expect(leave).toHaveBeenCalledWith(10, expect.anything());
  });

  it('refresca la distribución cuando se elimina una tarea del grupo', () => {
    const deleteTask = vi.spyOn(component.tasksStore, 'deleteTask').mockImplementation(() => undefined);
    const loadDistribution = vi.spyOn(component.metricsStore, 'loadGroupDistribution').mockImplementation(() => undefined);
    asLeader(10);
    component.groupId.set(10);

    component.onDeleteTask(7);
    deleteTask.mock.calls[0][1]?.();

    expect(deleteTask).toHaveBeenCalledWith(7, expect.any(Function));
    expect(loadDistribution).toHaveBeenCalledWith(10);
  });

  it('refresca el resumen de estado cuando cambia el estado de una tarea', () => {
    const updateStatus = vi.spyOn(component.tasksStore, 'updateTaskStatus').mockImplementation(() => undefined);
    const loadOverview = vi.spyOn(component.metricsStore, 'loadGroupOverview').mockImplementation(() => undefined);
    asLeader(10);
    component.groupId.set(10);

    component.onChangeTaskStatus({ taskId: 7, status: 'DONE' });
    updateStatus.mock.calls[0][2]?.();

    expect(updateStatus).toHaveBeenCalledWith(7, 'DONE', expect.any(Function));
    expect(loadOverview).toHaveBeenCalledWith(10);
  });

  it('un integrante solo refresca su propia productividad, no las métricas del grupo', () => {
    const updateStatus = vi.spyOn(component.tasksStore, 'updateTaskStatus').mockImplementation(() => undefined);
    const loadOverview = vi.spyOn(component.metricsStore, 'loadGroupOverview').mockImplementation(() => undefined);
    const loadMember = vi.spyOn(component.metricsStore, 'loadMember').mockImplementation(() => undefined);
    component.groupId.set(10);
    component.selectedMemberId.set(5);

    component.onChangeTaskStatus({ taskId: 7, status: 'DONE' });
    updateStatus.mock.calls[0][2]?.();

    expect(loadOverview).not.toHaveBeenCalled();
    expect(loadMember).toHaveBeenCalledWith(10, 5);
  });
});
