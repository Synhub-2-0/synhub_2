export interface TourChapter {
  id: string;
  title: string;
  /** Segundo del video donde empieza el capítulo */
  start: number;
  description: string;
  ai?: boolean;
}

export const TOUR_VIDEO_URL = '/recorrido/synhub-recorrido.mp4';
export const TOUR_POSTER_URL = '/recorrido/poster.jpg';
export const TOUR_DURATION = 149.5;

/** Tiempos tomados de la grabación (demo-video/chapters.json). Si se regraba el video, actualizar aquí. */
export const TOUR_CHAPTERS: TourChapter[] = [
  { id: 'login', title: 'Inicio de sesión', start: 0, description: 'Lisa Chen, líder del equipo de diseño, entra con su usuario y contraseña.' },
  { id: 'panel', title: 'Panel principal', start: 8.7, description: 'Resumen de sus grupos y el tablero Kanban con el flujo de trabajo de las tareas.' },
  { id: 'grupos', title: 'Grupos liderados', start: 14.4, description: 'Crea un grupo nuevo y abre uno existente: integrantes, código de invitación y su tablero.' },
  { id: 'tareas', title: 'Gestión de tareas', start: 32.6, description: 'Tareas del grupo con filtros por estado y badges de dificultad, contexto y urgencia con explicación al pasar el mouse.' },
  { id: 'ia-crear', title: 'IA · Sugerir al crear una tarea', start: 41.4, ai: true, description: 'La IA propone dificultad, contexto, urgencia y etiquetas; el tooltip explica por qué y todo es editable.' },
  { id: 'ia-informe', title: 'IA · Informe del equipo', start: 65.9, ai: true, description: 'Carga ponderada por dificultad, riesgos y recomendaciones con nombres y números reales del grupo.' },
  { id: 'detalle', title: 'Detalle de una tarea', start: 85.6, description: 'Responsable, grupo, fechas y la clasificación de la tarea con sus explicaciones.' },
  { id: 'miembro', title: 'Vista de miembro', start: 96.5, description: 'Isabel ve solo sus tareas, marca una como completada, pausa otra y revisa sus grupos.' },
  { id: 'seguimiento', title: 'Seguimiento del líder', start: 121.2, description: 'Lisa ve al instante el avance de Isabel en el tablero y en sus filtros.' },
  { id: 'cierre', title: 'Cierre de sesión', start: 144.2, description: 'Fin del recorrido.' },
];
