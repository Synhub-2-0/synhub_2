import { TaskDistribution } from '../domain/model/task-distribution.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';
import { TaskDistributionResource, RescheduledTasksResource } from './metrics.response';

export class MetricsAssembler {
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
