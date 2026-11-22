import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SubscriptionsApi } from './subscriptions.api';
import { ChangePlanCommand } from '../domain/model/change-plan.command';
import { environment } from '../../../environments/environment';

describe('SubscriptionsApi (HTTP)', () => {
  const url = `${environment.platformProviderApiBaseUrl}${environment.platformProviderSubscriptionsEndpointPath}/me`;
  const resource = {
    plan: 'FREE', planName: 'Gratis', monthlyPrice: 0, maxLedGroups: 2, activeLedGroups: 2, lockedLedGroups: 1, availableSlots: 0,
    plans: [
      { plan: 'FREE', name: 'Gratis', monthlyPrice: 0, maxLedGroups: 2 },
      { plan: 'PRO', name: 'Pro', monthlyPrice: 9, maxLedGroups: 5 },
      { plan: 'ULTRA', name: 'Ultra', monthlyPrice: 29, maxLedGroups: null },
    ],
  };
  let api: SubscriptionsApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(SubscriptionsApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('consulta mi plan en /subscriptions/me y lo convierte en entidad', () => {
    let plan = '';
    api.getMySubscription().subscribe((s) => (plan = s.plan));
    backend.expectOne({ method: 'GET', url }).flush(resource);
    expect(plan).toBe('FREE');
  });

  it('cambiar de plan envía solo el plan y los grupos que se conservan, sin datos de pago', () => {
    api.changePlan(new ChangePlanCommand('FREE', [10, 12])).subscribe();
    const put = backend.expectOne({ method: 'PUT', url });
    expect(put.request.body).toEqual({ plan: 'FREE', activeGroupIds: [10, 12] });
    put.flush(resource);

    api.changePlan(new ChangePlanCommand('PRO')).subscribe();
    const second = backend.expectOne({ method: 'PUT', url });
    expect(second.request.body).toEqual({ plan: 'PRO' });
    second.flush(resource);
  });

  it.each<[number, string, string]>([
    [400, '', 'El plan elegido no es válido.'],
    [409, 'The new plan allows 2 active groups: choose exactly 2 of the groups you lead that are currently active', 'Elige exactamente los grupos que seguirán activos con el nuevo plan.'],
    [500, '', 'No se pudo cambiar el plan.'],
  ])('al cambiar de plan, un %i se traduce a un mensaje en español', (status, backendMessage, expected) => {
    let message = '';
    api.changePlan(new ChangePlanCommand('FREE')).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne({ method: 'PUT', url }).flush({ message: backendMessage }, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

  it('si no se puede consultar el plan, el mensaje está en español', () => {
    let message = '';
    api.getMySubscription().subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne({ method: 'GET', url }).flush({}, { status: 500, statusText: 'Error' });
    expect(message).toBe('No se pudo cargar tu plan.');
  });
});
