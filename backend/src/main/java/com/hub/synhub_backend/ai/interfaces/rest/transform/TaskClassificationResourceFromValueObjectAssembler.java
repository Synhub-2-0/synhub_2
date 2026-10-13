package com.hub.synhub_backend.ai.interfaces.rest.transform;

import com.hub.synhub_backend.ai.domain.model.valueobjects.TaskClassification;
import com.hub.synhub_backend.ai.interfaces.rest.resources.TaskClassificationResource;

public class TaskClassificationResourceFromValueObjectAssembler {
    public static TaskClassificationResource toResourceFromValueObject(TaskClassification classification) {
        return new TaskClassificationResource(classification.labels(), classification.urgency(),
                classification.difficulty(), classification.context(), classification.rationale());
    }
}
