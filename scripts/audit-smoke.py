"""Prueba de humo de la auditoría (docs/AUDITORIA.md, anexo).

Repite los casos reproducidos en la auditoría contra la solución levantada con Docker:

    docker compose up --build -d
    python scripts/audit-smoke.py

Solo usa la biblioteca estándar. Registra tres usuarios nuevos en cada ejecución y revisa los logs del
contenedor mono-app-1 (tokens filtrados y errores 500). Termina con "TODOS OK" o marca con ✗ las diferencias.
"""
import http.cookiejar, json, random, subprocess, urllib.error, urllib.request
from datetime import datetime, timedelta, timezone

BASE = 'http://localhost:8080/api/v1'


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def call(self, method, path, body=None, headers=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(BASE + path, data=data, method=method,
                                     headers={'Content-Type': 'application/json', **(headers or {})})
        try:
            with self.opener.open(req) as r:
                raw = r.read().decode()
                return r.status, (json.loads(raw) if raw else None)
        except urllib.error.HTTPError as e:
            raw = e.read().decode()
            try:
                return e.code, json.loads(raw)
            except ValueError:
                return e.code, raw


suffix = str(random.randint(1000, 9999))


def user(name):
    c = Client()
    username = f'{name}{suffix}'
    c.call('POST', '/authentication/sign-up', {'username': username, 'name': name, 'surname': 'T', 'email': f'{username}@x.io', 'password': 'Secret123!'})
    status, me = c.call('POST', '/authentication/sign-in', {'username': username, 'password': 'Secret123!'})
    assert status == 200, (status, me)
    c.id = me['id']
    c.username = username
    return c


leader, member, outsider = user('leader'), user('member'), user('outsider')
anon = Client()
rows = []


def case(name, got, expected):
    rows.append((name, got, expected))


case('Registro con email duplicado', anon.call('POST', '/authentication/sign-up', {'username': 'otro' + suffix, 'name': 'x', 'surname': 'y', 'email': leader.username + '@x.io', 'password': 'Secret123!'})[0], 409)
case('Registro con contraseña "1" y email inválido', anon.call('POST', '/authentication/sign-up', {'username': 'debil' + suffix, 'name': 'x', 'surname': 'y', 'email': 'no-es-email', 'password': '1'})[0], 400)
st, created = anon.call('POST', '/authentication/sign-up', {'username': 'rol' + suffix, 'name': 'x', 'surname': 'y', 'email': f'rol{suffix}@x.io', 'password': 'Secret123!', 'accountRoles': ['ROLE_NOPE']})
case('Registro con un rol inexistente', f"{st} {created.get('accountRoles') if isinstance(created, dict) else ''}", "201 ['ROLE_USER']")
case('POST /groups sin name', leader.call('POST', '/groups', {'description': 'x'})[0], 400)
st, group = leader.call('POST', '/groups', {'name': 'Equipo', 'description': 'd'})
gid = group['id']
member.call('POST', '/groups/join', {'code': group['code']})
due = (datetime.now(timezone.utc) + timedelta(days=2)).isoformat()
case('POST /tasks sin title', leader.call('POST', '/tasks', {'description': 'x', 'dueDate': due, 'userId': member.id, 'groupId': gid})[0], 400)
st, task = leader.call('POST', '/tasks', {'title': 'T', 'description': 'd', 'dueDate': due, 'userId': member.id, 'groupId': gid})
tid = task['id']
member.call('PUT', f'/tasks/{tid}/status?status=COMPLETED')
st, req = member.call('POST', f'/tasks/{tid}/requests', {'description': 'listo', 'requestType': 'SUBMISSION'})
rid = req['id']
case('Ajeno: GET /invitations/group?groupId=', outsider.call('GET', f'/invitations/group?groupId={gid}')[0], 403)
case('Ajeno: GET /groups/{id}/members', outsider.call('GET', f'/groups/{gid}/members')[0], 403)
case('Ajeno: GET /tasks/group/{id}', outsider.call('GET', f'/tasks/group/{gid}')[0], 403)
case('Ajeno: GET /tasks/{id}', outsider.call('GET', f'/tasks/{tid}')[0], 403)
case('Ajeno: GET /tasks/{id}/requests', outsider.call('GET', f'/tasks/{tid}/requests')[0], 403)
case('Ajeno: GET /users', outsider.call('GET', '/users')[0], 403)
case('Ajeno: DELETE /tasks/{t}/requests/{r}', outsider.call('DELETE', f'/tasks/{tid}/requests/{rid}')[0], 403)
case('Líder: DELETE /tasks/{id} con requests', leader.call('DELETE', f'/tasks/{tid}')[0], 204)
st, t2 = leader.call('POST', '/tasks', {'title': 'T2', 'description': 'd', 'dueDate': due, 'userId': member.id, 'groupId': gid})
case('Líder: DELETE /groups/{id} con tareas', leader.call('DELETE', f'/groups/{gid}')[0], 204)
st, empty = leader.call('POST', '/groups', {'name': 'Vacio', 'description': 'd'})
case('Líder: DELETE /groups/{id} vacío', leader.call('DELETE', f"/groups/{empty['id']}")[0], 204)
token = next(c.value for c in leader.jar if c.name == 'SYNHUB_SESSION')
head, payload, sig = token.split('.')
payload = payload[:-2] + ('A' if payload[-2] != 'A' else 'B') + payload[-1]
case('Token alterado', anon.call('GET', '/users/me', headers={'Cookie': f'SYNHUB_SESSION={head}.{payload}.{sig}'})[0], 401)
logs = subprocess.run(['docker', 'logs', 'mono-app-1'], capture_output=True, text=True).stdout + subprocess.run(['docker', 'logs', 'mono-app-1'], capture_output=True, text=True).stderr
leaked = sum(logs.count(c.value) for cl in (leader, member, outsider) for c in cl.jar if c.name == 'SYNHUB_SESSION')
case('Tokens completos en docker logs', leaked, 0)
errors = logs.count('Unhandled exception')
case('Errores 500 en los logs durante la prueba', errors, 0)

ok = True
print('| Caso | Obtenido (rama audit) | Esperado |')
print('|---|---|---|')
for name, got, expected in rows:
    mark = '' if str(got) == str(expected) else ' ✗'
    ok &= not mark
    print(f'| {name} | {got}{mark} | {expected} |')
print('TODOS OK' if ok else 'HAY DIFERENCIAS')
raise SystemExit(0 if ok else 1)
