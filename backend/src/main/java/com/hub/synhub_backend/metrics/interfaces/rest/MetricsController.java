package com.hub.synhub_backend.metrics.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Provides access to analytics and group metrics")
public class MetricsController {

    public MetricsController() {
    }

    // TODO: Whatever this used to be (getGroupIdFromUser)

    // TODO: Average Task Time Passed

    // TODO: Get Task Overview for Group

    // TODO Task Distribution for Group

    // TODO: Get rescheduled tasks for a group

    // TODO: Get average completion time for group

    // TODO: Get task overview for member in a group

    // TODO: Get task distribution for member in a group

    // TODO: Get rescheduled tasks for member

    // TODO: Get average completion time for member in a group

    // TODO: Get duration of an IN_PROGRESS task
}