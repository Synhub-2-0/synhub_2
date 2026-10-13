package com.hub.synhub_backend.metrics.interfaces.rest.transform;

import com.hub.synhub_backend.metrics.domain.model.valueobjects.*;
import com.hub.synhub_backend.metrics.interfaces.rest.resources.*;
import java.util.stream.Collectors;

/** Convierte los value objects de metrics en sus recursos REST (AUDIT ARC-02). El JSON no cambia. */
public final class MetricsResourceFromValueAssembler {

    private MetricsResourceFromValueAssembler() {}

    public static TaskOverviewResource toResource(TaskOverview value) {
        return new TaskOverviewResource(value.type(), value.value(), value.details());
    }

    public static TaskDistributionResource toResource(TaskDistribution value) {
        var details = value.details().entrySet().stream()
                .collect(Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> new MemberTaskInfo(
                                entry.getValue().memberName(), entry.getValue().taskCount())));
        return new TaskDistributionResource(value.type(), value.value(), details);
    }

    public static RescheduledTasksResource toResource(RescheduledTasks value) {
        return new RescheduledTasksResource(value.type(), value.value(), value.details(), value.rescheduledMemberIds());
    }

    public static AvgCompletionTimeResource toResource(AvgCompletionTime value) {
        return new AvgCompletionTimeResource(value.type(), value.value(), value.details());
    }

    public static TaskTimePassedResource toResource(TaskTimePassed value) {
        return new TaskTimePassedResource(value.memberId(), value.timePassed());
    }

    public static TaskDurationResource toResource(TaskDuration value) {
        return new TaskDurationResource(value.taskId(), value.durationInHours());
    }
}
