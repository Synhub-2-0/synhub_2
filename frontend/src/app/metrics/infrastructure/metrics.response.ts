export interface MemberTaskInfoResource {
  memberName: string;
  taskCount: number;
}

export interface TaskDistributionResource {
  type: string;
  value: number;
  details: Record<string, MemberTaskInfoResource>;
}

export interface TaskOverviewResource {
  type: string;
  value: number;
  details: Record<string, number>;
}

export interface RescheduledTasksResource {
  type: string;
  value: number;
  details: Record<string, number>;
  rescheduledMemberIds: number[];
}
