package com.hub.synhub_backend.metrics.domain.services;

import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.domain.model.valueobjects.*;

public interface TaskMetricsQueryService {
    TaskTimePassed handle(GetTaskTimePassedForUserInGroupQuery query);

    AvgCompletionTime handle(GetAvgCompletionTimeInGroupQuery query);

    RescheduledTasks handle(GetRescheduledTasksInGroupQuery query);

    TaskDistribution handle(GetTaskDistributionInGroupQuery query);

    TaskOverview handle(GetTaskOverviewInGroupQuery query);

    TaskOverview handle(GetTaskOverviewForUserInGroupQuery query);

    TaskDistribution handle(GetTaskDistributionForUserInGroupQuery query);

    RescheduledTasks handle(GetRescheduledTasksForUserInGroupQuery query);

    AvgCompletionTime handle(GetAvgCompletionTimeForUserInGroupQuery query);

    TaskTimePassed handle(GetTaskTimePassedByIdQuery query);

    TaskDuration handle(GetTaskDurationByIdQuery query);
}
