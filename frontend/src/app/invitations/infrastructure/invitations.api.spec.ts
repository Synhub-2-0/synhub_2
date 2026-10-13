import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InvitationsApi } from './invitations.api';
import { CreateInvitationCommand } from '../domain/model/create-invitation.command';
import { environment } from '../../../environments/environment';

describe('InvitationsApi (HTTP)', () => {
  const url = `${environment.platformProviderApiBaseUrl}${environment.platformProviderInvitationsEndpointPath}`;
  const resource = {
    id: 50,
    group: { id: 10, name: 'Equipo', imgUrl: '', description: '', code: 'ABC123XYZ', memberCount: 1 },
    user: { id: 9, name: 'Luis', surname: 'T', email: '', imgUrl: '', userId: 9, username: 'luis' },
  };
  let api: InvitationsApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(InvitationsApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('URLs y verbos de cada operación', () => {
    api.createInvitation(new CreateInvitationCommand({ groupId: 10, userId: 9 })).subscribe();
    api.acceptInvitation(50).subscribe();
    api.declineInvitation(50).subscribe();
    api.getInvitationsByAuthenticatedUser().subscribe();
    api.getInvitationsByGroupId(10).subscribe();

    const post = backend.expectOne({ method: 'POST', url });
    expect(post.request.body).toEqual({ groupId: 10, userId: 9 });
    post.flush(resource);
    backend.expectOne({ method: 'POST', url: `${url}/accept?invitationId=50` }).flush(null);
    backend.expectOne({ method: 'DELETE', url: `${url}/50` }).flush(null);
    backend.expectOne({ method: 'GET', url: `${url}/user` }).flush([resource]);
    backend.expectOne({ method: 'GET', url: `${url}/group?groupId=10` }).flush({ invitations: [resource] });
  });

  it.each([
    [403, '', 'Solo el líder del grupo puede enviar invitaciones.'],
    [404, '', 'No se encontró el usuario o el grupo.'],
    [409, 'User with id 9 is already in group with id 10', 'Este usuario ya es miembro del grupo.'],
    [409, 'User with id 9 already has an invitation for group with id 10', 'Este usuario ya tiene una invitación pendiente.'],
    [500, '', 'No se pudo enviar la invitación.'],
  ])('al invitar, un %i se traduce a un mensaje en español', (status, backendMessage, expected) => {
    let message = '';
    api.createInvitation(new CreateInvitationCommand({ groupId: 10, userId: 9 })).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(url).flush({ message: backendMessage }, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });
});
