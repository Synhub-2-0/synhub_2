import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { Plans } from './plans';
import { SubscriptionsStore } from '../../../application/subscriptions.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { Subscription, Plan } from '../../../domain/model/subscription.entity';
import { ChangePlanCommand } from '../../../domain/model/change-plan.command';
import { Group } from '../../../../groups/domain/model/group.entity';

const catalog: Plan[] = [
  { code: 'FREE', name: 'Gratis', monthlyPrice: 0, maxLedGroups: 2 },
  { code: 'PRO', name: 'Pro', monthlyPrice: 9, maxLedGroups: 5 },
  { code: 'ULTRA', name: 'Ultra', monthlyPrice: 29, maxLedGroups: null },
];

const subscription = (plan: 'FREE' | 'PRO' | 'ULTRA', active: number, locked = 0) => new Subscription({
  plan, planName: catalog.find((p) => p.code === plan)!.name, monthlyPrice: 0,
  maxLedGroups: catalog.find((p) => p.code === plan)!.maxLedGroups, activeLedGroups: active, lockedLedGroups: locked,
  availableSlots: plan === 'ULTRA' ? null : Math.max(0, catalog.find((p) => p.code === plan)!.maxLedGroups! - active), plans: catalog,
});

const group = (id: number, locked = false) => new Group({ id, name: `G${id}`, imgUrl: '', description: '', code: `C${id}`, memberCount: 1, locked });

describe('Plans', () => {
  let load: ReturnType<typeof vi.fn>;
  let changePlan: ReturnType<typeof vi.fn>;
  let loadGroups: ReturnType<typeof vi.fn>;
  let current: ReturnType<typeof signal<Subscription | null>>;
  let leaderGroups: ReturnType<typeof signal<Group[]>>;
  let component: Plans;

  const create = async (sub: Subscription, groups: Group[]) => {
    current = signal<Subscription | null>(sub);
    leaderGroups = signal(groups);
    load = vi.fn();
    changePlan = vi.fn();
    loadGroups = vi.fn();
    await TestBed.configureTestingModule({
      imports: [Plans],
      providers: [
        provideRouter([]),
        { provide: SubscriptionsStore, useValue: { subscription: current, loading: signal(false), changing: signal(false), load, changePlan } },
        { provide: GroupsStore, useValue: { leaderGroups, loadGroups, loading: signal(false) } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(Plans);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  };

  afterEach(() => TestBed.resetTestingModule());

  it('al abrir carga mi plan y mis grupos', async () => {
    await create(subscription('FREE', 2), [group(1), group(2)]);
    expect(load).toHaveBeenCalled();
    expect(loadGroups).toHaveBeenCalled();
  });

  it('muestra los tres planes con su precio simulado y su límite', async () => {
    const fixture = await create(subscription('FREE', 1), [group(1)]);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Gratis');
    expect(text).toContain('$9');
    expect(text).toContain('$29');
    expect(component.limitLabel(catalog[0])).toBe('2 grupos liderados activos');
    expect(component.limitLabel(catalog[2])).toBe('Grupos liderados ilimitados');
    expect(component.priceLabel(catalog[0])).toBe('$0');
    expect(component.priceLabel(catalog[1])).toBe('$9 / mes');
    expect(component.isCurrent(catalog[0])).toBe(true);
    expect(component.isCurrent(catalog[1])).toBe(false);
  });

  it('el pago es simulado y lo dice', async () => {
    const fixture = await create(subscription('FREE', 1), [group(1)]);
    component.startCheckout(catalog[1]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Pago simulado');
  });

  it('subir de plan no pide elegir grupos y confirma el plan', async () => {
    await create(subscription('FREE', 2, 1), [group(1), group(2), group(3, true)]);
    component.startCheckout(catalog[1]);
    expect(component.needsSelection()).toBe(false);
    expect(component.canConfirm()).toBe(true);

    component.confirm();

    expect(changePlan).toHaveBeenCalledTimes(1);
    const command = changePlan.mock.calls[0][0] as ChangePlanCommand;
    expect([command.plan, command.activeGroupIds]).toEqual(['PRO', undefined]);
  });

  it('al terminar recarga los grupos y cierra el pago', async () => {
    await create(subscription('FREE', 1), [group(1)]);
    component.startCheckout(catalog[1]);
    component.confirm();
    changePlan.mock.calls[0][1]();
    expect(loadGroups).toHaveBeenCalledTimes(2);
    expect(component.selectedPlan()).toBeNull();
  });

  it('bajar a un límite menor pide elegir exactamente los grupos que siguen activos', async () => {
    await create(subscription('PRO', 3), [group(1), group(2), group(3)]);
    component.startCheckout(catalog[0]);

    expect(component.needsSelection()).toBe(true);
    expect(component.keptGroupIds()).toEqual([1, 2]); // sugiere los más antiguos
    expect(component.canConfirm()).toBe(true);

    component.toggleGroup(1);
    expect(component.keptGroupIds()).toEqual([2]);
    expect(component.canConfirm()).toBe(false);

    component.toggleGroup(3);
    expect(component.keptGroupIds()).toEqual([2, 3]);
    component.toggleGroup(1); // ya hay 2 elegidos: no deja pasarse
    expect(component.keptGroupIds()).toEqual([2, 3]);

    component.confirm();
    const command = changePlan.mock.calls[0][0] as ChangePlanCommand;
    expect([command.plan, command.activeGroupIds]).toEqual(['FREE', [2, 3]]);
  });

  it('los grupos bloqueados no se ofrecen para conservar', async () => {
    await create(subscription('PRO', 3, 1), [group(1), group(2), group(3), group(4, true)]);
    component.startCheckout(catalog[0]);
    expect(component.activeLedGroups().map((g) => g.id)).toEqual([1, 2, 3]);
    expect(component.keptGroupIds()).toEqual([1, 2]);
  });

  it('si los grupos activos caben en el plan nuevo no hay nada que elegir', async () => {
    await create(subscription('PRO', 2), [group(1), group(2)]);
    component.startCheckout(catalog[0]);
    expect(component.needsSelection()).toBe(false);
  });

  it('cancelar cierra el pago sin cambiar nada', async () => {
    await create(subscription('FREE', 1), [group(1)]);
    component.startCheckout(catalog[1]);
    component.cancelCheckout();
    expect(component.selectedPlan()).toBeNull();
    expect(changePlan).not.toHaveBeenCalled();
  });

  it('el plan actual no se vuelve a elegir', async () => {
    await create(subscription('FREE', 1), [group(1)]);
    component.startCheckout(catalog[0]);
    expect(component.selectedPlan()).toBeNull();
  });

  it('avisa cuántos grupos están bloqueados y cómo activarlos', async () => {
    const fixture = await create(subscription('PRO', 2, 2), [group(1), group(2), group(3, true), group(4, true)]);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('2 grupos bloqueados');
    expect(text).toContain('Grupos Liderados');
  });
});
