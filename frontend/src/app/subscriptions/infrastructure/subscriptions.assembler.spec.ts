import { SubscriptionsAssembler } from './subscriptions.assembler';

describe('SubscriptionsAssembler', () => {
  const assembler = new SubscriptionsAssembler();
  const resource = {
    plan: 'PRO', planName: 'Pro', monthlyPrice: 9, maxLedGroups: 5, activeLedGroups: 2, lockedLedGroups: 1, availableSlots: 3,
    plans: [
      { plan: 'FREE', name: 'Gratis', monthlyPrice: 0, maxLedGroups: 2 },
      { plan: 'ULTRA', name: 'Ultra', monthlyPrice: 29, maxLedGroups: null },
    ],
  };

  it('convierte el recurso en una suscripción con su catálogo de planes', () => {
    const subscription = assembler.toEntityFromResource(resource as never);
    expect(subscription.plan).toBe('PRO');
    expect(subscription.planName).toBe('Pro');
    expect(subscription.maxLedGroups).toBe(5);
    expect(subscription.availableSlots).toBe(3);
    expect(subscription.plans.map((p) => p.code)).toEqual(['FREE', 'ULTRA']);
    expect(subscription.plans[1].maxLedGroups).toBeNull();
  });

  it('un plan sin límite no tiene cupos que contar', () => {
    const ultra = assembler.toEntityFromResource({ ...resource, plan: 'ULTRA', planName: 'Ultra', maxLedGroups: null, availableSlots: null } as never);
    expect(ultra.isUnlimited).toBe(true);
    expect(ultra.hasFreeSlot).toBe(true);
    expect(assembler.toEntityFromResource(resource as never).isUnlimited).toBe(false);
  });

  it('hasFreeSlot es falso cuando no quedan cupos', () => {
    const full = assembler.toEntityFromResource({ ...resource, plan: 'FREE', maxLedGroups: 2, activeLedGroups: 2, availableSlots: 0 } as never);
    expect(full.hasFreeSlot).toBe(false);
  });

  it('el comando de cambio de plan solo lleva el plan y los grupos elegidos', () => {
    expect(assembler.toRequestFromCommand({ plan: 'FREE', activeGroupIds: [1, 2] })).toEqual({ plan: 'FREE', activeGroupIds: [1, 2] });
    expect(assembler.toRequestFromCommand({ plan: 'PRO' })).toEqual({ plan: 'PRO' });
  });
});
