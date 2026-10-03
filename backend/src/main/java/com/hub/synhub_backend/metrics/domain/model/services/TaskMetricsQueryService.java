package com.hub.synhub_backend.metrics.domain.model.services;

import com.hub.synhub_backend.metrics.domain.model.queries.*;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;

public interface TaskMetricsQueryService {
    TaskTimePassedResource handle(GetTaskTimePassedForUserInGroupQuery query);
    AvgCompletionTimeResource handle(GetAvgCompletionTimeInGroupQuery query);
    RescheduledTasksResource handle(GetRescheduledTasksInGroupQuery query);
    TaskDistributionResource handle(GetTaskDistributionInGroupQuery query);
    TaskOverviewResource handle(GetTaskOverviewInGroupQuery query);
    TaskOverviewResource handle(GetTaskOverviewForUserInGroupQuery query);
    TaskDistributionResource handle(GetTaskDistributionForUserInGroupQuery query);
    RescheduledTasksResource handle(GetRescheduledTasksForUserInGroupQuery query);
    AvgCompletionTimeResource handle(GetAvgCompletionTimeForUserInGroupQuery query);
    TaskTimePassedResource handle(GetTaskTimePassedByIdQuery query);
    TaskDurationResource handle(GetTaskDurationByIdQuery query);
}