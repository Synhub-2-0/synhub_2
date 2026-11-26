export interface TaskOverview {
  type: string;
  total: number;
  details: Record<string, number>;
}
