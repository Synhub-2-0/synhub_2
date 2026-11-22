import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { GroupsStore } from '../../../application/groups.store';
import { SubscriptionsStore } from '../../../../subscriptions/application/subscriptions.store';
import { Subscription } from '../../../../subscriptions/domain/model/subscription.entity';
import { GroupCard } from './group-card';
import { Group } from '../../../domain/model/group.entity';

describe('GroupCard', () => {
  let component: GroupCard;
  let fixture: ComponentFixture<GroupCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupCard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('group', new Group({
      id: 1, name: 'Diseño & Branding Collab', imgUrl: '', description: 'Equipo de diseño', code: 'ABC123', memberCount: 6,
    }));
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el detalle se abre en /groups/{id} sin datos de rol en la URL', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.openGroupDetails();
    expect(navigate).toHaveBeenCalledWith(['/groups', 1]);
  });

  it('shows the group name', () => {
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Diseño & Branding Collab');
  });
});

describe('GroupCard con el grupo bloqueado', () => {
  let activateGroup: ReturnType<typeof vi.fn>;
  let load: ReturnType<typeof vi.fn>;

  const sub = (availableSlots: number | null) => new Subscription({
    plan: 'FREE', planName: 'Gratis', monthlyPrice: 0, maxLedGroups: 2, activeLedGroups: 2 - (availableSlots ?? 0), lockedLedGroups: 1, availableSlots, plans: [],
  });

  const render = async (props: { locked: boolean; isLeader: boolean; subscription: Subscription | null }) => {
    TestBed.resetTestingModule();
    activateGroup = vi.fn();
    load = vi.fn();
    await TestBed.configureTestingModule({
      imports: [GroupCard],
      providers: [
        provideRouter([]),
        { provide: GroupsStore, useValue: { activateGroup } },
        { provide: SubscriptionsStore, useValue: { subscription: signal(props.subscription), load } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(GroupCard);
    fixture.componentRef.setInput('group', new Group({ id: 3, name: 'Soporte', imgUrl: '', description: 'd', code: 'XYZ', memberCount: 2, locked: props.locked }));
    fixture.componentRef.setInput('isLeader', props.isLeader);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  };

  const text = (fixture: { nativeElement: unknown }) => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const activateButton = (fixture: { nativeElement: unknown }) =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => b.textContent?.includes('Activar grupo'));

  it('un grupo activo no muestra insignia ni botón de activar', async () => {
    const fixture = await render({ locked: false, isLeader: true, subscription: sub(0) });
    expect(text(fixture)).not.toContain('Bloqueado');
    expect(activateButton(fixture)).toBeUndefined();
  });

  it('un grupo bloqueado lo marca como "Bloqueado" para todos', async () => {
    const asMember = await render({ locked: true, isLeader: false, subscription: null });
    expect(text(asMember)).toContain('Bloqueado');
    expect(activateButton(asMember)).toBeUndefined();
  });

  it('el líder con cupo puede activarlo y se actualizan sus cupos', async () => {
    const fixture = await render({ locked: true, isLeader: true, subscription: sub(1) });
    const button = activateButton(fixture) as HTMLButtonElement;
    expect(button.disabled).toBe(false);

    button.click();

    expect(activateGroup).toHaveBeenCalledWith(3, expect.any(Function));
    activateGroup.mock.calls[0][1]();
    expect(load).toHaveBeenCalled();
  });

  it('sin cupo el botón está deshabilitado y explica cómo liberar uno', async () => {
    const fixture = await render({ locked: true, isLeader: true, subscription: sub(0) });
    expect((activateButton(fixture) as HTMLButtonElement).disabled).toBe(true);
    expect(text(fixture)).toContain('Sin cupo');
  });

  it('el líder de un grupo bloqueado carga su plan si aún no lo tiene', async () => {
    await render({ locked: true, isLeader: true, subscription: null });
    expect(load).toHaveBeenCalled();
  });

  it('activar no abre el detalle del grupo', async () => {
    const fixture = await render({ locked: true, isLeader: true, subscription: sub(1) });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    (activateButton(fixture) as HTMLButtonElement).click();
    expect(navigate).not.toHaveBeenCalled();
  });
});

