export interface MemberTaskInfo {
  memberName: string;
  taskCount: number;
}

export interface TaskDistribution {
  type: string;
  total: number;
  details: Record<string, MemberTaskInfo>;
}
