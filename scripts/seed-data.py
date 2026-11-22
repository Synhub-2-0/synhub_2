"""Datos de prueba de SynHub, cargados solo a través de la API REST.

Con el backend levantado (por ejemplo `.\\scripts\\run-backend.ps1`):

    python scripts/seed-data.py [URL_BASE]      # por defecto http://localhost:8080/api/v1

Es idempotente: si un usuario ya existe inicia sesión con él, si el grupo ya existe lo reutiliza y si
alguien ya es miembro lo omite. Todos los usuarios usan la contraseña PASSWORD. Solo usa la biblioteca estándar.

Usuarios y grupos (L = líder, M = miembro):

    usuario          Desarrollo Web   Marketing Digital   Diseño UX
    alexiel (ya existe)    M                M                 L
    maria.lopez            L                M                 -      -> líder y miembro
    carlos.ruiz            -                L                 -      -> solo líder
    lucia.torres           M                -                 M      -> solo miembro
    diego.paredes          M                -                 M
    sofia.vega             -                M                 M
"""
import http.cookiejar
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080/api/v1'
PASSWORD = 'hola1234'

NEW_USERS = [
    ('maria.lopez', 'María', 'López'),
    ('carlos.ruiz', 'Carlos', 'Ruiz'),
    ('lucia.torres', 'Lucía', 'Torres'),
    ('diego.paredes', 'Diego', 'Paredes'),
    ('sofia.vega', 'Sofía', 'Vega'),
]

# (nombre, descripción, líder, miembros)
GROUPS = [
    ('Desarrollo Web', 'Equipo del portal web: frontend, backend y despliegue.',
     'maria.lopez', ['alexiel', 'lucia.torres', 'diego.paredes']),
    ('Marketing Digital', 'Campañas, redes sociales y analítica de la marca.',
     'carlos.ruiz', ['maria.lopez', 'alexiel', 'sofia.vega']),
    ('Diseño UX', 'Investigación de usuarios, prototipos y sistema de diseño.',
     'alexiel', ['sofia.vega', 'diego.paredes', 'lucia.torres']),
]


class Client:
    def __init__(self, username):
        self.username = username
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, body=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(BASE + path, data=data, method=method,
                                     headers={'Content-Type': 'application/json'})
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

    def sign_in(self):
        status, body = self.call('POST', '/authentication/sign-in',
                                 {'username': self.username, 'password': PASSWORD})
        if status != 200:
            sys.exit(f'No se pudo iniciar sesión como {self.username}: {status} {body}')
        return body


def main():
    clients = {}

    for username, name, surname in NEW_USERS:
        c = Client(username)
        status, body = c.call('POST', '/authentication/sign-up', {
            'username': username, 'name': name, 'surname': surname,
            'email': f'{username}@synhub.test', 'password': PASSWORD})
        if status in (200, 201):
            print(f'usuario creado:   {username}')
        elif status == 409:
            print(f'usuario ya existe: {username}')
        else:
            sys.exit(f'Registro de {username} falló: {status} {body}')
        clients[username] = c

    clients['alexiel'] = Client('alexiel')
    for c in clients.values():
        c.sign_in()

    for name, description, leader, members in GROUPS:
        lc = clients[leader]
        _, led = lc.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        group = next((g for g in (led or []) if g['name'] == name), None)
        if group:
            print(f'grupo ya existe:  {name} (líder {leader})')
        else:
            status, group = lc.call('POST', '/groups', {'name': name, 'description': description})
            if status not in (200, 201):
                sys.exit(f'Crear grupo {name} falló: {status} {group}')
            print(f'grupo creado:     {name} (líder {leader}, código {group["code"]})')

        for m in members:
            status, body = clients[m].call('POST', '/groups/join', {'code': group['code']})
            if status in (200, 201):
                print(f'  + {m} se unió a {name}')
            elif status == 409:
                print(f'  = {m} ya estaba en {name}')
            else:
                sys.exit(f'{m} no pudo unirse a {name}: {status} {body}')

    print('\nResumen por usuario:')
    for username, c in clients.items():
        _, led = c.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        _, mem = c.call('GET', '/groups/user/role?groupRole=GROUP_MEMBER')
        print(f'  {username:14} líder de {[g["name"] for g in led or []]}  miembro de {[g["name"] for g in mem or []]}')


main()
