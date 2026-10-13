import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Validations } from './validations';
import { RequestsApi } from '../../../infrastructure/requests.api';
import { GroupsApi } from '../../../../groups/infrastructure/groups.api';
import { IamApi } from '../../../../iam/infrastructure/iam.api';

const apiStub = () => new Proxy({} as Record<string, ReturnType<typeof vi.fn>>, {
  get: (target, prop: string) => (target[prop] ??= vi.fn(() => of([]))),
});

describe('Validations', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Validations],
      providers: [
        provideRouter([]),
        { provide: RequestsApi, useValue: apiStub() },
        { provide: GroupsApi, useValue: apiStub() },
        { provide: IamApi, useValue: apiStub() },
      ],
    }).compileComponents();
  });

  it('se crea y ofrece los filtros de estado', async () => {
    const fixture = TestBed.createComponent(Validations);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.componentInstance.statusOptions.map((o) => o.value)).toEqual(['PENDING', 'APPROVED', 'REJECTED']);
  });
});
