"""Datos de prueba de SynHub (usuarios, grupos, invitaciones, tareas y entregas), cargados solo por la API REST.

Con el backend levantado (por ejemplo `.\\scripts\\run-backend.ps1`):

    python scripts/seed-data.py [URL_BASE]      # por defecto http://localhost:8080/api/v1

Es idempotente: reutiliza usuarios, grupos, membresías y tareas que ya existen (las tareas se buscan por título
dentro del grupo). Todos los usuarios usan la contraseña PASSWORD. Solo usa la biblioteca estándar.

Usuarios (L = líder, M = miembro). `alexiel` ya debe existir:

    usuario          Desarrollo  Marketing  Diseño UX  Soporte  Finanzas  RRHH  Operaciones
    alexiel               M          M          L         L        M        M       L      -> líder y miembro, 7 grupos
    maria.lopez           L          M          -         M        L        -       -      -> líder y miembro
    carlos.ruiz           -          L          -         -        -        L       -      -> solo líder
    lucia.torres          M          -          M         M        -        M       M      -> solo miembro
    diego.paredes         M          -          M         M        M        -       -      -> solo miembro
    sofia.vega            -          M          M         M        -        M       M      -> solo miembro

Tareas: cada grupo trae tareas en todos los estados para probar el tablero y las validaciones:

    progress      En progreso (sin más)             hold        En espera
    completed     Completada, sin entrega enviada   submitted   Completada + entrega PENDING (para validar)
    approved      Entrega aprobada -> DONE          rejected    Entrega rechazada -> vuelve a En progreso
    modification  En progreso + solicitud de modificación PENDING
    expired       Fecha límite pasada: el planificador del backend la vence en <= 60 s y crea su request EXPIRED

"Soporte Técnico" tiene 24 tareas para probar la paginación (20 por página). Además deja invitaciones pendientes.
"""
import http.cookiejar
import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

sys.stdout.reconfigure(encoding='utf-8')

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
    ('Soporte Técnico', 'Atención de tickets e incidencias de clientes.',
     'alexiel', ['maria.lopez', 'lucia.torres', 'diego.paredes', 'sofia.vega']),
    ('Finanzas', 'Contabilidad, conciliaciones y presupuesto.',
     'maria.lopez', ['alexiel', 'diego.paredes']),
    ('Recursos Humanos', 'Selección, onboarding y clima laboral.',
     'carlos.ruiz', ['lucia.torres', 'sofia.vega', 'alexiel']),
    ('Operaciones', 'Inventario, logística y auditoría de procesos.',
     'alexiel', ['sofia.vega', 'lucia.torres']),
]

# Invitaciones pendientes: (grupo, quién invita, a quién). Siguen sin aceptar para poder probar aceptar y rechazar.
INVITATIONS = [
    ('Finanzas', 'maria.lopez', 'lucia.torres'),
    ('Finanzas', 'maria.lopez', 'sofia.vega'),
    ('Operaciones', 'alexiel', 'carlos.ruiz'),
    ('Operaciones', 'alexiel', 'maria.lopez'),
    ('Recursos Humanos', 'carlos.ruiz', 'diego.paredes'),
]

# (título, descripción, asignado, horas hasta el vencimiento, clasificación, estado)
# clasificación = (dificultad, contexto, urgencia, etiquetas) o None
TASKS = {
    'Desarrollo Web': [
        ('Maquetar página de inicio', 'Landing con hero, beneficios y llamada a la acción.', 'lucia.torres', 72, (2, 'FRONTEND', 'MEDIUM', 'UI, Landing'), 'progress'),
        ('Endpoint de autenticación', 'Inicio de sesión con cookie HttpOnly y refresco.', 'diego.paredes', 48, (4, 'BACKEND', 'HIGH', 'API, Seguridad'), 'submitted'),
        ('Migración de esquema', 'Agregar columnas de auditoría con Flyway.', 'alexiel', 120, (3, 'DATABASE', 'MEDIUM', 'Flyway'), 'hold'),
        ('Configurar pipeline CI', 'Compilar, probar y publicar en cada merge.', 'lucia.torres', 24, (3, 'CLOUD', 'HIGH', 'CI/CD'), 'completed'),
        ('Pruebas de regresión', 'Suite E2E de los flujos críticos.', 'diego.paredes', 96, (2, 'QA', 'LOW', 'Testing'), 'approved'),
        ('Refactor del carrito', 'Separar la lógica de precios del controlador.', 'alexiel', 60, (4, 'BACKEND', 'MEDIUM', 'Refactor'), 'rejected'),
        ('Documentar la API', 'Describir los endpoints públicos en Swagger.', 'lucia.torres', -48, (1, 'DOCS', 'LOW', 'Docs'), 'expired'),
        ('Optimizar consultas lentas', 'Revisar los índices de las tablas más grandes.', 'diego.paredes', 30, None, 'modification'),
    ],
    'Marketing Digital': [
        ('Calendario de contenidos', 'Plan editorial del mes para todas las redes.', 'maria.lopez', 72, (2, 'GENERAL', 'LOW', 'Planificación'), 'progress'),
        ('Campaña de lanzamiento', 'Piezas gráficas y textos del lanzamiento.', 'sofia.vega', 48, (3, 'DESIGN', 'HIGH', 'Campaña, Diseño'), 'submitted'),
        ('Informe de analítica', 'Resumen de métricas de tráfico y conversión.', 'alexiel', 96, (3, 'DATABASE', 'MEDIUM', 'Analítica'), 'hold'),
        ('Publicaciones de redes', 'Programar las publicaciones de hoy.', 'maria.lopez', 1, (1, 'GENERAL', 'HIGH', 'Social'), 'progress'),
        ('Guía de estilo de marca', 'Tono de voz, colores y tipografías.', 'sofia.vega', 120, (2, 'DOCS', 'LOW', 'Guía estilo, Documentación'), 'approved'),
    ],
    'Diseño UX': [
        ('Prototipo de alta fidelidad', 'Pantallas clave del flujo de registro.', 'sofia.vega', 96, (4, 'DESIGN', 'HIGH', 'Figma, Prototipo'), 'progress'),
        ('Entrevistas a usuarios', 'Cinco entrevistas con el público objetivo.', 'diego.paredes', 72, (2, 'GENERAL', 'MEDIUM', 'Research'), 'submitted'),
        ('Sistema de diseño', 'Componentes base, tokens y documentación.', 'lucia.torres', 144, (5, 'DESIGN', 'MEDIUM', 'Componentes'), 'hold'),
        ('Revisión de accesibilidad', 'Contraste, foco y lectores de pantalla.', 'sofia.vega', -24, (3, 'QA', 'HIGH', 'WCAG'), 'expired'),
    ],
    'Finanzas': [
        ('Cierre contable mensual', 'Cuadrar libros y emitir el balance.', 'alexiel', 72, (3, 'GENERAL', 'HIGH', 'Contabilidad'), 'submitted'),
        ('Conciliación bancaria', 'Cruzar movimientos con los extractos.', 'diego.paredes', 48, (2, 'DATABASE', 'MEDIUM', 'Finanzas'), 'progress'),
        ('Presupuesto 2027', 'Proyección de ingresos y gastos por área.', 'alexiel', 240, (5, 'GENERAL', 'MEDIUM', 'Planificación'), 'hold'),
    ],
    'Recursos Humanos': [
        ('Onboarding de nuevos ingresos', 'Kit de bienvenida y accesos.', 'lucia.torres', 72, (2, 'DOCS', 'MEDIUM', 'RRHH'), 'approved'),
        ('Evaluación de desempeño', 'Formularios y calendario de entrevistas.', 'sofia.vega', 120, (3, 'GENERAL', 'LOW', 'Evaluación'), 'progress'),
        ('Encuesta de clima laboral', 'Diseñar, enviar y tabular la encuesta.', 'alexiel', 48, (2, 'GENERAL', 'MEDIUM', 'Encuesta'), 'modification'),
    ],
    'Operaciones': [
        ('Inventario trimestral', 'Conteo físico y ajuste de existencias.', 'sofia.vega', 96, (3, 'GENERAL', 'MEDIUM', 'Inventario'), 'progress'),
        ('Auditoría de procesos', 'Levantar y revisar los procesos de bodega.', 'lucia.torres', 72, (2, 'QA', 'LOW', 'Auditoría'), 'completed'),
    ],
}

# Para probar la paginación: 24 tickets con estados variados.
_SUPPORT_ASSIGNEES = ['maria.lopez', 'lucia.torres', 'diego.paredes', 'sofia.vega']
TASKS['Soporte Técnico'] = [
    (f'Ticket #{i:02d}: incidencia de cliente', f'Atender y cerrar la incidencia {i}.',
     _SUPPORT_ASSIGNEES[i % 4], 6 * i, (1 + i % 5, ['BACKEND', 'FRONTEND', 'DATABASE', 'QA'][i % 4], ['LOW', 'MEDIUM', 'HIGH'][i % 3], 'Soporte'),
     'submitted' if i % 10 == 0 else 'hold' if i % 6 == 0 else 'progress')
    for i in range(1, 25)
]


class Client:
    def __init__(self, username):
        self.username = username
        self.id = None
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
        self.id = body['id']


REQUEST_STATES = ('submitted', 'approved', 'rejected', 'modification')


def ok(status):
    return status in (200, 201, 204)


def seed_users(clients):
    for username, name, surname in NEW_USERS:
        c = Client(username)
        status, body = c.call('POST', '/authentication/sign-up', {
            'username': username, 'name': name, 'surname': surname,
            'email': f'{username}@synhub.test', 'password': PASSWORD})
        if ok(status):
            print(f'usuario creado:    {username}')
        elif status == 409:
            print(f'usuario ya existe: {username}')
        else:
            sys.exit(f'Registro de {username} falló: {status} {body}')
        clients[username] = c
    clients['alexiel'] = Client('alexiel')
    for c in clients.values():
        c.sign_in()


def seed_groups(clients):
    groups = {}
    for name, description, leader, members in GROUPS:
        lc = clients[leader]
        _, led = lc.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        group = next((g for g in (led or []) if g['name'] == name), None)
        if group:
            print(f'grupo ya existe:   {name} (líder {leader})')
        else:
            status, group = lc.call('POST', '/groups', {'name': name, 'description': description})
            if not ok(status):
                sys.exit(f'Crear grupo {name} falló: {status} {group}')
            print(f'grupo creado:      {name} (líder {leader}, código {group["code"]})')
        groups[name] = group
        for m in members:
            status, body = clients[m].call('POST', '/groups/join', {'code': group['code']})
            if ok(status):
                print(f'  + {m} se unió a {name}')
            elif status != 409:
                sys.exit(f'{m} no pudo unirse a {name}: {status} {body}')
    return groups


def seed_invitations(clients, groups):
    for group_name, inviter, invited in INVITATIONS:
        status, body = clients[inviter].call(
            'POST', '/invitations', {'userId': clients[invited].id, 'groupId': groups[group_name]['id']})
        if ok(status):
            print(f'invitación:        {invited} -> {group_name}')
        elif status != 409:
            print(f'  ! invitación {invited} -> {group_name}: {status} {body}')


def apply_state(clients, leader, assignee, task, state):
    """Lleva la tarea (recién creada, EN PROGRESO) al estado pedido usando solo la API."""
    tid = task['id']
    member = clients[assignee]
    if state == 'hold':
        member.call('PUT', f'/tasks/{tid}/status?status=ON_HOLD')
    elif state in ('completed', 'submitted', 'approved', 'rejected'):
        member.call('PUT', f'/tasks/{tid}/status?status=COMPLETED')
        if state == 'completed':
            return
        status, req = member.call('POST', '/requests',
                                  {'taskId': tid, 'description': 'Entrega lista para revisión.', 'requestType': 'SUBMISSION'})
        if not ok(status):
            print(f'  ! entrega de "{task["title"]}": {status} {req}')
            return
        if state in ('approved', 'rejected'):
            verdict = 'APPROVED' if state == 'approved' else 'REJECTED'
            status, body = leader.call('PUT', f'/requests/{req["id"]}/status/{verdict}')
            if not ok(status):
                print(f'  ! revisión de "{task["title"]}": {status} {body}')
    elif state == 'modification':
        status, body = member.call('POST', '/requests',
                                   {'taskId': tid, 'description': 'Necesito ajustar el alcance de la tarea.', 'requestType': 'MODIFICATION'})
        if not ok(status):
            print(f'  ! modificación de "{task["title"]}": {status} {body}')


def seed_tasks(clients, groups):
    now = datetime.now(timezone.utc)
    for group_name, tasks in TASKS.items():
        group = groups[group_name]
        leader = clients[next(g[2] for g in GROUPS if g[0] == group_name)]
        _, existing = leader.call('GET', f'/tasks/group/{group["id"]}')
        known = {t['title']: t for t in (existing or [])}
        created = 0
        for title, description, assignee, hours, cls, state in tasks:
            if title in known:
                # Reintento: si le faltó su entrega o solicitud (p. ej. una corrida anterior falló), se completa.
                if state in REQUEST_STATES:
                    _, reqs = leader.call('GET', f'/requests/task?taskId={known[title]["id"]}')
                    if not reqs:
                        apply_state(clients, leader, assignee, known[title], state)
                continue
            body = {'title': title, 'description': description,
                    'dueDate': (now + timedelta(hours=hours)).isoformat(),
                    'userId': clients[assignee].id, 'groupId': group['id']}
            if cls:
                body.update(difficulty=cls[0], context=cls[1], urgency=cls[2], labels=cls[3])
            status, task = leader.call('POST', '/tasks', body)
            if not ok(status):
                print(f'  ! tarea "{title}" en {group_name}: {status} {task}')
                continue
            apply_state(clients, leader, assignee, task, state)
            created += 1
        print(f'tareas {group_name}: {created} nuevas, {len(known)} ya existían')


def summary(clients, groups):
    print('\nResumen por usuario:')
    for username, c in clients.items():
        _, led = c.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        _, mem = c.call('GET', '/groups/user/role?groupRole=GROUP_MEMBER')
        print(f'  {username:14} líder de {[g["name"] for g in led or []]}  miembro de {[g["name"] for g in mem or []]}')
    print('\nTareas por grupo y estado:')
    for name, group in groups.items():
        leader = clients[next(g[2] for g in GROUPS if g[0] == name)]
        _, tasks = leader.call('GET', f'/tasks/group/{group["id"]}')
        counts = {}
        for t in tasks or []:
            counts[t['status']] = counts.get(t['status'], 0) + 1
        print(f'  {name:18} {len(tasks or []):2} tareas  {counts}')
    print('\nLas tareas "expired" pasan a EXPIRED en <= 60 s (planificador del backend).')


def main():
    clients = {}
    seed_users(clients)
    groups = seed_groups(clients)
    seed_invitations(clients, groups)
    seed_tasks(clients, groups)
    summary(clients, groups)


main()
