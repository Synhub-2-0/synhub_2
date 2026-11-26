"""Datos de prueba de SynHub (usuarios, grupos, invitaciones, tareas y entregas), cargados solo por la API REST.

Con el backend levantado (por ejemplo `.\\scripts\\run-backend.ps1`):

    python scripts/seed-data.py [URL_BASE]      # por defecto http://localhost:8080/api/v1

Es idempotente: reutiliza usuarios, grupos, membresías y tareas que ya existen (las tareas se buscan por título
dentro del grupo). Todos los usuarios usan la contraseña PASSWORD. Solo usa la biblioteca estándar.

Usuarios (L = líder, M = miembro). Si `alexiel` no existe, se crea con PASSWORD:

    usuario          Desarrollo  Marketing  Diseño UX  Soporte  Finanzas  RRHH  Operaciones
    alexiel               M          M          L         L        M        M       L      -> líder y miembro, 7 grupos
    maria.lopez           L          M          -         M        L        -       -      -> líder y miembro
    carlos.ruiz           -          L          -         -        -        L       -      -> solo líder
    lucia.torres          M          -          M         M        -        M       M      -> solo miembro
    diego.paredes         M          -          M         M        M        -       -      -> solo miembro
    sofia.vega            -          M          M         M        -        M       M      -> solo miembro

Tareas: cada grupo trae tareas en todos los estados para probar el tablero y las validaciones:

    progress      En progreso (sin más)             hold        En espera
    completed     Completada por el líder, sin entrega   submitted   Completada por la entrega PENDING (para validar)
    approved      Entrega aprobada -> DONE          rejected    Entrega rechazada -> vuelve a En progreso
    modification  En espera + solicitud de modificación PENDING (el líder la edita o la deniega)
    modification_denied  Solicitud de modificación denegada -> vuelve a En progreso
    expired       Fecha límite pasada: el planificador del backend la vence en <= 60 s y crea su request EXPIRED
                  (el líder solo puede reanudarla con una nueva fecha: en Validaciones o al mover la tarea en el tablero)

    rejected_approved  Entrega rechazada (reagendado +1), reentregada y aprobada -> DONE
    rejected_twice     Rechazada dos veces (reagendados +2) y aprobada a la tercera -> DONE

"Soporte Técnico" tiene 24 tareas para probar la paginación (20 por página). Además deja invitaciones pendientes.

Métricas (US-012, US-021): cada grupo trae además un historial de tareas terminadas, reagendadas y vencidas
(HISTORY_TASKS). Como la API cierra esas tareas segundos después de crearlas, el "cierre promedio" saldría en 0 días:
el último paso (seed_metrics_history) retrocede por SQL (`psql`, credenciales DATABASE_* del entorno o del `.env` de la
raíz) la creación y el tiempo activo de las tareas DONE/COMPLETED y vencidas. Cada integrante tiene un ritmo distinto
(MEMBER_PACE) para que la vista de productividad individual compare perfiles:

    lucia.torres  rápida, sin vencidas      diego.paredes  lento, reagenda y vence
    sofia.vega    media                     alexiel / maria.lopez  medios, con algún reagendado

Si no hay `psql`, el paso se salta y lo avisa: el resto de los datos queda igual.

Planes (suscripciones): primero se suben a PRO/ULTRA para poder cargar todos los grupos y, al final, `alexiel` baja a
Gratis (2 grupos activos) conservando Diseño UX y Soporte Técnico: **Operaciones queda bloqueado** (solo lectura) con sus
tareas, para probar el bloqueo, "Activar grupo" y la pantalla "Mi plan".

    usuario          plan final   grupos liderados
    alexiel          Gratis       3 (Operaciones bloqueado)
    maria.lopez      Pro          2 activos
    carlos.ruiz      Gratis       2 activos (en su límite)
    diego.paredes    Ultra        ninguno (sin límite)
    lucia.torres, sofia.vega      Gratis, sin grupos liderados
"""
import glob
import http.cookiejar
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

sys.stdout.reconfigure(encoding='utf-8')

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080/api/v1'
PASSWORD = 'hola1234'

NEW_USERS = [
    ('alexiel', 'Alexiel', 'Demo'),  # si ya existe, se reutiliza (con la contraseña PASSWORD)
    ('maria.lopez', 'María', 'López'),
    ('carlos.ruiz', 'Carlos', 'Ruiz'),
    ('lucia.torres', 'Lucía', 'Torres'),
    ('diego.paredes', 'Diego', 'Paredes'),
    ('sofia.vega', 'Sofía', 'Vega'),
]

# Plan con el que se carga cada usuario que lidera grupos (el resto queda en Gratis) y el plan final.
PLANS_WHILE_SEEDING = {'alexiel': 'PRO', 'maria.lopez': 'PRO', 'carlos.ruiz': 'FREE', 'diego.paredes': 'ULTRA'}
# Al terminar, `alexiel` baja a Gratis conservando estos grupos: el que sobra queda bloqueado.
ALEXIEL_KEEPS = ['Diseño UX', 'Soporte Técnico']

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
        ('Internacionalizar textos', 'Extraer los textos a archivos de idioma.', 'lucia.torres', 84, (3, 'FRONTEND', 'LOW', 'i18n'), 'modification_denied'),
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
        # Con entrega y modificación pendientes: al final el grupo queda bloqueado y se ven sin acciones.
        ('Conteo de bodega norte', 'Contar y registrar las existencias de la bodega norte.', 'sofia.vega', 60, (2, 'GENERAL', 'MEDIUM', 'Inventario'), 'submitted'),
        ('Reubicar estantes', 'Reorganizar los estantes por rotación.', 'lucia.torres', 96, (3, 'GENERAL', 'LOW', 'Logística'), 'modification'),
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

# Historial para las métricas: tareas cerradas (con su entrega aprobada), reagendadas y vencidas.
# Mismo formato que TASKS; se agregan al final de cada grupo.
HISTORY_TASKS = {
    'Desarrollo Web': [
        ('Diseñar esquema de base de datos', 'Modelo entidad-relación del portal.', 'diego.paredes', 200, (4, 'DATABASE', 'HIGH', 'Modelado'), 'rejected_twice'),
        ('Configurar entorno de staging', 'Servidor de pruebas con datos anonimizados.', 'lucia.torres', 220, (3, 'CLOUD', 'MEDIUM', 'DevOps'), 'approved'),
        ('Formulario de contacto', 'Validaciones y envío por correo.', 'lucia.torres', 180, (1, 'FRONTEND', 'LOW', 'UI'), 'approved'),
        ('Integrar pasarela de pagos', 'Cobros con tarjeta y webhooks de confirmación.', 'alexiel', 260, (5, 'BACKEND', 'HIGH', 'Pagos'), 'rejected_approved'),
        ('Corregir bugs del sprint 3', 'Cerrar los bugs abiertos del último sprint.', 'alexiel', 150, (2, 'QA', 'MEDIUM', 'Bugs'), 'approved'),
        ('Página de preguntas frecuentes', 'Sección FAQ con buscador.', 'diego.paredes', -72, (1, 'FRONTEND', 'LOW', 'Contenido'), 'expired'),
        ('Auditoría de rendimiento', 'Lighthouse y tiempos de respuesta de la API.', 'diego.paredes', -30, (3, 'QA', 'MEDIUM', 'Rendimiento'), 'expired'),
    ],
    'Marketing Digital': [
        ('Campaña de email', 'Secuencia de bienvenida para nuevos suscriptores.', 'sofia.vega', 200, (2, 'DESIGN', 'MEDIUM', 'Email'), 'approved'),
        ('Reporte trimestral de marca', 'Alcance, interacción y sentimiento del trimestre.', 'alexiel', 240, (3, 'GENERAL', 'MEDIUM', 'Reporte'), 'rejected_approved'),
        ('SEO on-page', 'Títulos, metadescripciones y enlaces internos.', 'maria.lopez', 170, (2, 'FRONTEND', 'LOW', 'SEO'), 'approved'),
        ('Video promocional', 'Guion y edición de un video de 30 segundos.', 'sofia.vega', -96, (4, 'DESIGN', 'HIGH', 'Video'), 'expired'),
    ],
    'Diseño UX': [
        ('Mapa de empatía', 'Síntesis de las entrevistas por perfil.', 'sofia.vega', 190, (2, 'DESIGN', 'MEDIUM', 'Research'), 'approved'),
        ('Wireframes del checkout', 'Flujo de pago en baja fidelidad.', 'lucia.torres', 210, (3, 'DESIGN', 'HIGH', 'Wireframes'), 'rejected_approved'),
        ('Guía de iconografía', 'Estilo, grilla y exportación de íconos.', 'diego.paredes', 230, (2, 'DESIGN', 'LOW', 'Íconos'), 'approved'),
        ('Test de usabilidad', 'Sesiones moderadas con cinco usuarios.', 'lucia.torres', 160, (3, 'QA', 'MEDIUM', 'Usabilidad'), 'approved'),
        ('Benchmark de competidores', 'Comparar los flujos de registro de tres competidores.', 'diego.paredes', -50, (2, 'GENERAL', 'LOW', 'Research'), 'expired'),
    ],
    'Soporte Técnico': [
        (f'Ticket cerrado #{i:02d}', f'Incidencia {i} resuelta y confirmada por el cliente.',
         _SUPPORT_ASSIGNEES[i % 4], 150 + 10 * i, (1 + i % 3, 'BACKEND', 'MEDIUM', 'Soporte'),
         'rejected_approved' if i % 3 == 0 else 'approved')
        for i in range(1, 7)
    ] + [
        ('Ticket vencido: caída del correo', 'El cliente reporta que no recibe correos.', 'diego.paredes', -20, (3, 'CLOUD', 'HIGH', 'Soporte'), 'expired'),
        ('Ticket vencido: factura duplicada', 'Revisar el doble cobro reportado.', 'sofia.vega', -60, (2, 'DATABASE', 'MEDIUM', 'Soporte'), 'expired'),
    ],
    'Finanzas': [
        ('Reporte de flujo de caja', 'Entradas y salidas del mes anterior.', 'diego.paredes', 200, (3, 'GENERAL', 'MEDIUM', 'Reporte'), 'rejected_approved'),
        ('Pago a proveedores', 'Programar los pagos de la quincena.', 'alexiel', 160, (1, 'GENERAL', 'HIGH', 'Pagos'), 'approved'),
        ('Declaración de impuestos', 'Preparar y presentar la declaración mensual.', 'diego.paredes', -40, (4, 'GENERAL', 'HIGH', 'Impuestos'), 'expired'),
    ],
    'Recursos Humanos': [
        ('Publicar vacantes', 'Avisos en portales de empleo y LinkedIn.', 'sofia.vega', 180, (1, 'GENERAL', 'MEDIUM', 'Selección'), 'approved'),
        ('Plan de capacitación', 'Cursos del semestre por área.', 'lucia.torres', 220, (3, 'DOCS', 'LOW', 'Capacitación'), 'approved'),
        ('Actualizar reglamento interno', 'Incorporar la política de trabajo remoto.', 'alexiel', -36, (2, 'DOCS', 'MEDIUM', 'Políticas'), 'expired'),
    ],
}
for _group, _tasks in HISTORY_TASKS.items():
    TASKS[_group] = TASKS[_group] + _tasks

# Ritmo de cada integrante (multiplica los días activos de sus tareas cerradas): menor es más rápido.
MEMBER_PACE = {'lucia.torres': 0.6, 'alexiel': 0.9, 'sofia.vega': 1.0, 'carlos.ruiz': 1.0,
               'maria.lopez': 1.1, 'diego.paredes': 1.6}


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


REQUEST_STATES = ('submitted', 'approved', 'rejected', 'rejected_approved', 'rejected_twice',
                  'modification', 'modification_denied')
# Revisiones que recibe cada entrega sucesiva, por estado: un rechazo vuelve la tarea a En progreso (+1 reagendado).
SUBMISSION_REVIEWS = {
    'submitted': [None],
    'approved': ['APPROVED'],
    'rejected': ['REJECTED'],
    'rejected_approved': ['REJECTED', 'APPROVED'],
    'rejected_twice': ['REJECTED', 'REJECTED', 'APPROVED'],
}


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
    for c in clients.values():
        c.sign_in()


def seed_plans(clients):
    """Sube de plan a quien lidere más grupos que los del plan Gratis y activa los grupos que hubieran quedado bloqueados
    (una corrida anterior deja uno bloqueado): así se pueden cargar tareas y miembros en todos."""
    for username, plan in PLANS_WHILE_SEEDING.items():
        status, body = clients[username].call('PUT', '/subscriptions/me', {'plan': plan})
        if not ok(status):
            sys.exit(f'Cambiar el plan de {username} a {plan} falló: {status} {body}')
        print(f'plan:              {username} -> {plan}')
    for username in PLANS_WHILE_SEEDING:
        c = clients[username]
        _, led = c.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        for group in led or []:
            if group.get('locked'):
                status, body = c.call('POST', f'/groups/{group["id"]}/activate')
                print(f'  grupo activado:  {group["name"]}' if ok(status) else f'  ! no se pudo activar {group["name"]}: {status} {body}')


def finish_plans(clients, groups):
    """`alexiel` baja a Gratis conservando dos grupos: el tercero (Operaciones) queda bloqueado."""
    keep = [groups[name]['id'] for name in ALEXIEL_KEEPS]
    status, body = clients['alexiel'].call('PUT', '/subscriptions/me', {'plan': 'FREE', 'activeGroupIds': keep})
    if not ok(status):
        sys.exit(f'Bajar a alexiel al plan Gratis falló: {status} {body}')
    print(f'plan:              alexiel -> FREE (activos: {", ".join(ALEXIEL_KEEPS)})')


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
    # Solo el líder cambia estados. El integrante entrega con POST /requests y el backend completa la tarea.
    if state == 'hold':
        leader.call('PUT', f'/tasks/{tid}/status?status=ON_HOLD')
    elif state == 'completed':
        leader.call('PUT', f'/tasks/{tid}/status?status=COMPLETED')
    elif state in SUBMISSION_REVIEWS:
        for attempt, verdict in enumerate(SUBMISSION_REVIEWS[state]):
            text = 'Entrega lista para revisión.' if attempt == 0 else 'Corregí lo observado, va de nuevo.'
            status, req = member.call('POST', '/requests',
                                      {'taskId': tid, 'description': text, 'requestType': 'SUBMISSION'})
            if not ok(status):
                print(f'  ! entrega de "{task["title"]}": {status} {req}')
                return
            if verdict is None:
                return
            status, body = leader.call('PUT', f'/requests/{req["id"]}/status/{verdict}')
            if not ok(status):
                print(f'  ! revisión de "{task["title"]}": {status} {body}')
                return
    elif state in ('modification', 'modification_denied'):
        # El backend deja la tarea en espera (su reloj se detiene) hasta que el líder la edite o la deniegue.
        status, req = member.call('POST', '/requests',
                                  {'taskId': tid, 'description': 'Necesito ajustar el alcance de la tarea.', 'requestType': 'MODIFICATION'})
        if not ok(status):
            print(f'  ! modificación de "{task["title"]}": {status} {req}')
            return
        if state == 'modification_denied':
            status, body = leader.call('PUT', f'/requests/{req["id"]}/status/REJECTED')  # la tarea vuelve a En progreso
            if not ok(status):
                print(f'  ! denegar la modificación de "{task["title"]}": {status} {body}')


def normalize_modification(clients, leader, assignee, task):
    """Deja la tarea de estado `modification` como hoy la deja el backend: en espera con una solicitud pendiente.

    Una corrida anterior (antes de que la solicitud pusiera la tarea en espera) pudo dejar la solicitud pendiente con la
    tarea en progreso o completada: se deniega, se devuelve la tarea a En progreso y se vuelve a pedir la modificación."""
    tid = task['id']
    _, reqs = leader.call('GET', f'/requests/task?taskId={tid}')
    pending = [r for r in reqs or [] if r['requestType'] == 'MODIFICATION' and r['requestStatus'] == 'PENDING']
    _, current = leader.call('GET', f'/tasks/{tid}')
    if pending and current['status'] == 'ON_HOLD':
        return
    for r in pending:
        leader.call('PUT', f'/requests/{r["id"]}/status/REJECTED')
        print(f'  ~ solicitud pendiente de "{task["title"]}" denegada (estaba inconsistente)')
    if current['status'] != 'IN_PROGRESS':
        leader.call('PUT', f'/tasks/{tid}/status?status=IN_PROGRESS')
    apply_state(clients, leader, assignee, task, 'modification')


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
                if state == 'modification':
                    normalize_modification(clients, leader, assignee, known[title])
                # Reintento: si le faltó su entrega o solicitud (p. ej. una corrida anterior falló), se completa.
                elif state in REQUEST_STATES:
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


def database_settings():
    """DATABASE_URL/USERNAME/PASSWORD del entorno o, si faltan, del `.env` de la raíz del repo (como run-backend.ps1)."""
    values = {}
    env_file = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), '.env')
    if os.path.exists(env_file):
        with open(env_file, encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    values[key.strip()] = value.strip().strip('"')
    get = lambda key, default: os.environ.get(key) or values.get(key) or default
    url = get('DATABASE_URL', 'jdbc:postgresql://localhost:5432/postgres')
    m = re.match(r'jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)', url)
    if not m:
        return None
    return {'host': m.group(1), 'port': m.group(2) or '5432', 'db': m.group(3),
            'user': get('DATABASE_USERNAME', 'postgres'), 'password': get('DATABASE_PASSWORD', 'admin')}


def find_psql():
    found = shutil.which('psql')
    if found:
        return found
    candidates = sorted(glob.glob(r'C:\Program Files\PostgreSQL\*\bin\psql.exe'))
    return candidates[-1] if candidates else None


def seed_metrics_history():
    """Retrocede en el tiempo las tareas cerradas y vencidas para que las métricas de plazos tengan valores reales.

    Solo toca filas que aún no se retrocedieron (tiempo activo < 1 h o creadas después de su fecha límite): es idempotente.
    - DONE/COMPLETED: días activos = (dificultad * 0.9 + id % 4 * 0.4) * ritmo del integrante; cerradas hace 1-9 días.
    - Fecha límite pasada: creadas 3-7 días antes de vencer. Si ya están EXPIRED, el tiempo activo cubre hasta la fecha
      límite; si el planificador aún no las venció, su reloj arranca en la creación y él lo cierra al vencerlas."""
    psql, db = find_psql(), database_settings()
    if not psql or not db:
        print('\n! Historial de métricas omitido: no se encontró psql o DATABASE_URL no es de PostgreSQL.')
        return
    pace = ', '.join(f"('{u}', {p})" for u, p in MEMBER_PACE.items())
    sql = f"""
with pace(username, factor) as (values {pace}),
closed as (
    select t.id,
           (coalesce(t.difficulty, 2) * 0.9 + (t.id % 4) * 0.4) * coalesce(pace.factor, 1) as active_days,
           ((t.id % 9) + 1)::int as closed_days_ago
    from tasks t
    join group_profiles gp on gp.id = t.profile_id
    join users u on u.id = gp.user_id
    left join pace on pace.username = u.username
    where t.status in ('DONE', 'COMPLETED') and t.time_passed < 3600000)
update tasks t
set time_passed = (c.active_days * 86400000)::bigint,
    created_at  = now() - make_interval(secs => (c.active_days + c.closed_days_ago) * 86400),
    updated_at  = now() - make_interval(days => c.closed_days_ago)
from closed c where c.id = t.id;

update tasks
set created_at = due_date - make_interval(days => 3 + (id % 5)::int)
where due_date < now() and created_at > due_date;

update tasks
set time_passed = extract(epoch from (due_date - created_at))::bigint * 1000
where status = 'EXPIRED' and time_passed < 3600000;

update tasks
set active_since = created_at
where status = 'IN_PROGRESS' and due_date < now() and active_since > due_date;

select count(*) filter (where status in ('DONE', 'COMPLETED')) || ' cerradas, '
       || count(*) filter (where due_date < now() and status <> 'DONE') || ' vencidas o por vencer'
from tasks where deleted_at is null;
"""
    env = dict(os.environ, PGPASSWORD=db['password'], PGCLIENTENCODING='UTF8', LC_MESSAGES='C')
    result = subprocess.run(
        [psql, '-h', db['host'], '-p', db['port'], '-U', db['user'], '-d', db['db'], '-w', '-q', '-t', '-A',
         '-v', 'ON_ERROR_STOP=1'],
        input=sql, text=True, capture_output=True, env=env, encoding='utf-8', errors='replace')
    if result.returncode != 0:
        print(f'\n! Historial de métricas falló: {result.stderr.strip()}')
        return
    print(f'\nhistorial de métricas: {result.stdout.strip()} (base {db["db"]})')


def metrics_summary(clients, groups):
    print('\nMétricas por grupo (lo que ve el líder en el detalle del grupo):')
    for name, group in groups.items():
        leader = clients[next(g[2] for g in GROUPS if g[0] == name)]
        _, avg = leader.call('GET', f'/metrics/groups/{group["id"]}/avg-completion-time')
        _, overview = leader.call('GET', f'/metrics/groups/{group["id"]}/overview')
        _, resched = leader.call('GET', f'/metrics/groups/{group["id"]}/rescheduled')
        if not isinstance(avg, dict) or not isinstance(overview, dict):
            print(f'  {name:18} (sin acceso: {avg})')
            continue
        total = overview.get('value', 0)
        expired = overview.get('details', {}).get('EXPIRED', 0)
        print(f'  {name:18} cierre promedio {avg["value"]:4.1f} d · terminadas {avg["details"].get("completedTasks", 0):2}'
              f' · vencidas {expired}/{total} ({(expired / total * 100) if total else 0:.0f} %)'
              f' · reagendados {resched.get("value", 0) if isinstance(resched, dict) else "?"}')


def summary(clients, groups):
    print('\nResumen por usuario:')
    for username, c in clients.items():
        _, led = c.call('GET', '/groups/user/role?groupRole=GROUP_LEADER')
        _, mem = c.call('GET', '/groups/user/role?groupRole=GROUP_MEMBER')
        _, plan = c.call('GET', '/subscriptions/me')
        locked = [g['name'] for g in led or [] if g.get('locked')]
        print(f'  {username:14} plan {plan["planName"]:6} líder de {[g["name"] for g in led or []]}'
              f'  miembro de {[g["name"] for g in mem or []]}' + (f'  BLOQUEADOS: {locked}' if locked else ''))
    print('\nTareas por grupo y estado:')
    for name, group in groups.items():
        leader = clients[next(g[2] for g in GROUPS if g[0] == name)]
        _, tasks = leader.call('GET', f'/tasks/group/{group["id"]}')
        counts = {}
        for t in tasks or []:
            counts[t['status']] = counts.get(t['status'], 0) + 1
        print(f'  {name:18} {len(tasks or []):2} tareas  {counts}')
    print('\nSolicitudes pendientes por grupo (lo que ve el líder en Validaciones):')
    for name, group in groups.items():
        leader = clients[next(g[2] for g in GROUPS if g[0] == name)]
        _, pending = leader.call('GET', f'/requests/group?groupId={group["id"]}&status=PENDING')
        counts = {}
        for r in pending or []:
            counts[r['requestType']] = counts.get(r['requestType'], 0) + 1
        print(f'  {name:18} {counts or "-"}')
    print('\nLas tareas "expired" pasan a EXPIRED en <= 60 s (planificador del backend).')


def main():
    clients = {}
    seed_users(clients)
    seed_plans(clients)
    groups = seed_groups(clients)
    seed_invitations(clients, groups)
    seed_tasks(clients, groups)
    seed_metrics_history()
    finish_plans(clients, groups)
    summary(clients, groups)
    metrics_summary(clients, groups)


main()
