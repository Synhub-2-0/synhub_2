import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { LeaderGroups } from './leader-groups';
import { GroupsStore } from '../../../application/groups.store';
import { Group } from '../../../domain/model/group.entity';

const group = (id: number) => new Group({ id, name: `Equipo ${id}`, imgUrl: '', description: 'd', code: 'ABC123XYZ', memberCount: 1 });

describe('LeaderGroups', () => {
  let component: LeaderGroups;
  let fixture: ComponentFixture<LeaderGroups>;
  let addGroup: ReturnType<typeof vi.fn>;
  let loadGroups: ReturnType<typeof vi.fn>;

  const setup = async (queryParams: Record<string, string> = {}) => {
    addGroup = vi.fn();
    loadGroups = vi.fn();
    await TestBed.configureTestingModule({
      imports: [LeaderGroups],
      providers: [
        {
          provide: GroupsStore,
          useValue: {
            loadGroups,
            addGroup,
            leaderGroups: signal([group(1)]),
            loading: signal(false),
            groups: signal([group(1)]),
          },
        },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
        { provide: Router, useValue: { navigate: vi.fn().mockResolvedValue(true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeaderGroups);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  it('should create', async () => {
    await setup();
    expect(component).toBeTruthy();
  });

  it('abre el modal de creación con ?create=true en la URL', async () => {
    await setup({ create: 'true' });
    expect(component.showCreateModal()).toBe(true);
  });

  it('toggleCreateModal alterna la visibilidad del modal', async () => {
    await setup();
    expect(component.showCreateModal()).toBe(false);
    component.toggleCreateModal();
    expect(component.showCreateModal()).toBe(true);
    component.toggleCreateModal();
    expect(component.showCreateModal()).toBe(false);
  });

  it('onCreateGroup no hace nada si el nombre o la descripción están vacíos', async () => {
    await setup();
    component.name = '  ';
    component.description = 'desc';
    component.onCreateGroup();
    expect(addGroup).not.toHaveBeenCalled();

    component.name = 'Equipo';
    component.description = '';
    component.onCreateGroup();
    expect(addGroup).not.toHaveBeenCalled();
  });

  it('onCreateGroup llama al store con los datos y limpia el formulario', async () => {
    await setup();
    component.name = '  Nuevo Equipo  ';
    component.description = '  Una descripción  ';
    component.imgUrl = 'https://img';
    component.showCreateModal.set(true);

    component.onCreateGroup();

    expect(addGroup).toHaveBeenCalledOnce();
    const command = addGroup.mock.calls[0][0];
    expect([command.name, command.description, command.imgUrl]).toEqual(['Nuevo Equipo', 'Una descripción', 'https://img']);
    expect(component.showCreateModal()).toBe(false);
    expect(component.name).toBe('');
    expect(component.description).toBe('');
  });
});
