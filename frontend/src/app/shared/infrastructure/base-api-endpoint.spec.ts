import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BaseApiEndpoint } from './base-api-endpoint';
import { BaseAssembler } from './base-assembler';
import { BaseResource, BaseResponse } from './base-response';
import { BaseEntity } from '../domain/model/base-entity';

interface NoteResource extends BaseResource { id: number; text: string; }
interface NotesResponse extends BaseResponse { notes: NoteResource[]; }
class Note implements BaseEntity { constructor(public id: number, public text: string) {} }

class NotesAssembler implements BaseAssembler<Note, NoteResource, NotesResponse> {
  toEntityFromResource(r: NoteResource): Note { return new Note(r.id, r.text); }
  toResourceFromEntity(e: Note): NoteResource { return { id: e.id, text: e.text }; }
  toEntitiesFromResponse(r: NotesResponse): Note[] { return r.notes.map((n) => this.toEntityFromResource(n)); }
}

class NotesEndpoint extends BaseApiEndpoint<Note, NoteResource, NotesResponse, NotesAssembler> {
  constructor(http: HttpClient) { super(http, '/api/notes', new NotesAssembler()); }
}

describe('BaseApiEndpoint', () => {
  let endpoint: NotesEndpoint;
  let backend: HttpTestingController;

  beforeEach(() => {
    vi.spyOn(console, 'log').mockImplementation(() => undefined);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    endpoint = new NotesEndpoint(TestBed.inject(HttpClient));
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('getAll acepta un arreglo o un objeto envoltorio', () => {
    const results: Note[][] = [];
    endpoint.getAll().subscribe((n) => results.push(n));
    endpoint.getAll().subscribe((n) => results.push(n));
    const [first, second] = backend.match('/api/notes');
    first.flush([{ id: 1, text: 'a' }]);
    second.flush({ notes: [{ id: 2, text: 'b' }, { id: 3, text: 'c' }] });
    expect(results.map((r) => r.map((n) => n.id))).toEqual([[1], [2, 3]]);
  });

  it('getById, create, update y delete usan la URL y el verbo correctos', () => {
    endpoint.getById(7).subscribe();
    endpoint.create(new Note(0, 'nueva')).subscribe();
    endpoint.update(new Note(7, 'editada'), 7).subscribe();
    endpoint.delete(7).subscribe();

    const get = backend.expectOne({ method: 'GET', url: '/api/notes/7' });
    const post = backend.expectOne({ method: 'POST', url: '/api/notes' });
    const put = backend.expectOne({ method: 'PUT', url: '/api/notes/7' });
    const del = backend.expectOne({ method: 'DELETE', url: '/api/notes/7' });
    expect(post.request.body).toEqual({ id: 0, text: 'nueva' });
    expect(put.request.body).toEqual({ id: 7, text: 'editada' });
    [get, post, put].forEach((r) => r.flush({ id: 7, text: 'x' }));
    del.flush(null);
  });

  it('handleError: 404 → "Resource not found"; otros → el código', () => {
    const messages: string[] = [];
    endpoint.getById(1).subscribe({ error: (e: Error) => messages.push(e.message) });
    endpoint.getById(2).subscribe({ error: (e: Error) => messages.push(e.message) });
    backend.expectOne('/api/notes/1').flush({}, { status: 404, statusText: 'Not Found' });
    backend.expectOne('/api/notes/2').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(messages).toEqual([
      'Failed to fetch entity with id 1: Resource not found',
      'Failed to fetch entity with id 2: 403',
    ]);
  });

  it('handleError: un error de red usa el mensaje del ErrorEvent', () => {
    let message = '';
    endpoint.delete(9).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne('/api/notes/9').error(new ErrorEvent('network', { message: 'sin conexión' }));
    expect(message).toBe('Failed to delete entity with id 9: sin conexión');
  });
});
