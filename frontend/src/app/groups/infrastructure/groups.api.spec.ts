import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { GroupsApi } from './groups.api';
import { Group } from '../domain/model/group.entity';
import { CreateGroupCommand } from '../domain/model/create-group.command';
import { environment } from '../../../environments/environment';

describe('GroupsApi (HTTP)', () => {
  const url = `${environment.platformProviderApiBaseUrl}${environment.platformProviderGroupsEndpointPath}`;
  const resource = { id: 1, name: 'Equipo', imgUrl: '', description: 'd', code: 'ABC123XYZ', memberCount: 2 };
  let api: GroupsApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(GroupsApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('grupos del usuario por rol', () => {
    let leader: Group[] = [];
    api.getLeaderGroups().subscribe((g) => (leader = g));
    api.getMemberGroups().subscribe();
    backend.expectOne(`${url}/user/role?groupRole=GROUP_LEADER`).flush([resource]);
    backend.expectOne(`${url}/user/role?groupRole=GROUP_MEMBER`).flush({ groups: [] });
    expect(leader.map((g) => g.id)).toEqual([1]);
  });

  it('crear, editar e integrantes', () => {
    api.createGroup(new CreateGroupCommand({ name: 'N', description: 'D', imgUrl: '' })).subscribe();
    api.updateGroup(new Group(resource)).subscribe();
    api.getGroupMembers(1).subscribe();

    const post = backend.expectOne({ method: 'POST', url });
    expect(post.request.body).toEqual({ name: 'N', description: 'D', imgUrl: '' });
    post.flush(resource);
    const put = backend.expectOne({ method: 'PUT', url: `${url}/1` });
    expect(put.request.body).toMatchObject({ name: 'Equipo', description: 'd', imgUrl: '' });
    put.flush(resource);
    backend.expectOne({ method: 'GET', url: `${url}/1/members` }).flush([]);
  });

  it('un error conserva el código en el mensaje', () => {
    let message = '';
    api.createGroup(new CreateGroupCommand({ name: '', description: '', imgUrl: '' })).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(url).flush({}, { status: 400, statusText: 'Bad Request' });
    expect(message).toBe('Failed to create group: 400');
  });

  it('activar un grupo bloqueado hace POST /groups/{id}/activate', () => {
    let activated: Group | undefined;
    api.activateGroup(7).subscribe((g) => (activated = g));
    const post = backend.expectOne({ method: 'POST', url: `${url}/7/activate` });
    expect(post.request.body).toBeNull();
    post.flush({ ...resource, id: 7, locked: false });
    expect(activated?.id).toBe(7);
    expect(activated?.locked).toBe(false);
  });

  it.each<[number, string]>([
    [403, 'Solo el líder del grupo puede activarlo.'],
    [409, 'No tienes cupo para activar otro grupo: elimina uno de tus grupos activos o sube de plan.'],
    [500, 'No se pudo activar el grupo.'],
  ])('al activar, un %i se traduce a un mensaje en español', (status, expected) => {
    let message = '';
    api.activateGroup(7).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne({ method: 'POST', url: `${url}/7/activate` }).flush({}, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

  it('eliminar un grupo hace DELETE /groups/{id}', () => {
    let done = false;
    api.deleteGroup(7).subscribe(() => (done = true));
    backend.expectOne({ method: 'DELETE', url: `${url}/7` }).flush(null, { status: 204, statusText: 'No Content' });
    expect(done).toBe(true);
  });

  it.each<[number, string]>([
    [403, 'Solo el líder del grupo puede eliminarlo.'],
    [404, 'El grupo ya no existe.'],
    [500, 'No se pudo eliminar el grupo.'],
  ])('al eliminar, un %i se traduce a un mensaje en español', (status, expected) => {
    let message = '';
    api.deleteGroup(7).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne({ method: 'DELETE', url: `${url}/7` }).flush({}, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

  it('HUM-05: unirse con código y quitar integrantes', () => {
    let joined: Group | undefined;
    api.joinGroupByCode('ABC123XYZ').subscribe((g) => (joined = g));
    api.removeMember(1, 5).subscribe();

    const join = backend.expectOne({ method: 'POST', url: `${url}/join` });
    expect(join.request.body).toEqual({ code: 'ABC123XYZ' });
    join.flush(resource);
    backend.expectOne({ method: 'DELETE', url: `${url}/1/members/5` }).flush(null);
    expect(joined?.id).toBe(1);
  });
});
