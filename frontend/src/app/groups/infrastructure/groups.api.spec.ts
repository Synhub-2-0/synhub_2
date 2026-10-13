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
});
