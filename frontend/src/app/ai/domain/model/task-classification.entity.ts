export type TaskContext = 'FRONTEND' | 'BACKEND' | 'DATABASE' | 'CLOUD' | 'QA' | 'DOCS' | 'DESIGN' | 'GENERAL';
export type TaskUrgency = 'LOW' | 'MEDIUM' | 'HIGH';

export const TASK_CONTEXTS: TaskContext[] = ['FRONTEND', 'BACKEND', 'DATABASE', 'CLOUD', 'QA', 'DOCS', 'DESIGN', 'GENERAL'];
export const TASK_URGENCIES: TaskUrgency[] = ['LOW', 'MEDIUM', 'HIGH'];

export const DIFFICULTY_SCALE: Record<number, string> = {
  1: 'Trivial: minutos',
  2: 'Fácil: pocas horas',
  3: 'Media: aprox. un día',
  4: 'Difícil: varios días o con riesgo',
  5: 'Muy compleja: incertidumbre alta',
};

/** Sugerencia de la IA para una tarea; el usuario puede editarla antes de guardar. */
export interface TaskClassification {
  labels: string[];
  urgency: TaskUrgency;
  difficulty: number;
  context: TaskContext;
  rationale: string;
}

export interface GroupReport {
  text: string;
  generatedAt: Date;
}

export interface ReportBlock {
  type: 'heading' | 'item' | 'paragraph';
  segments: { text: string; bold: boolean }[];
}

/** Convierte el markdown mínimo del informe (títulos, listas, negritas) en bloques renderizables sin innerHTML. */
export function parseReport(markdown: string): ReportBlock[] {
  const segments = (text: string) =>
    text.split('**').map((part, i) => ({ text: part, bold: i % 2 === 1 })).filter((s) => s.text);
  return markdown
    .split('\n')
    .filter((line) => line.trim())
    .map((line): ReportBlock => {
      if (/^#+\s/.test(line)) return { type: 'heading', segments: segments(line.replace(/^#+\s/, '')) };
      if (/^\s*(-|\d+\.)\s/.test(line)) return { type: 'item', segments: segments(line.replace(/^\s*(-|\d+\.)\s/, '')) };
      return { type: 'paragraph', segments: segments(line) };
    });
}
