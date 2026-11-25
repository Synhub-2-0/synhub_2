import { TaskDistribution } from '../domain/model/task-distribution.model';
import { TaskOverview } from '../domain/model/task-overview.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';
import { AvgCompletionTime } from '../domain/model/avg-completion-time.model';
import {
  AvgCompletionTimeResource,
  TaskDistributionResource,
  TaskOverviewResource,
  RescheduledTasksResource,
} from './metrics.response';

export class MetricsAssembler {
  toAvgCompletionTime(resource: AvgCompletionTimeResource): AvgCompletionTime {
    return {
      type: resource.type,
      days: resource.value,
      completedTasks: resource.details.completedTasks ?? 0,
    };
  }

  toTaskOverview(resource: TaskOverviewResource): TaskOverview {
    return {
      type: resource.type,
      total: resource.value,
      details: resource.details,
    };
  }

  toTaskDistribution(resource: TaskDistributionResource): TaskDistribution {
    return {
      type: resource.type,
      total: resource.value,
      details: resource.details,
    };
  }

  toRescheduledTasks(resource: RescheduledTasksResource): RescheduledTasks {
    return {
      type: resource.type,
      rescheduled: resource.value,
      total: resource.details['total'] ?? 0,
      rescheduledMemberIds: resource.rescheduledMemberIds ?? [],
    };
  }
}
