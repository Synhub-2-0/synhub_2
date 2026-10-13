package com.hub.synhub_backend.ai.interfaces.rest.transform;

import com.hub.synhub_backend.ai.domain.model.queries.ClassifyTaskQuery;
import com.hub.synhub_backend.ai.interfaces.rest.resources.ClassifyTaskResource;

public class ClassifyTaskQueryFromResourceAssembler {
    public static ClassifyTaskQuery toQueryFromResource(ClassifyTaskResource resource) {
        return new ClassifyTaskQuery(resource.title(), resource.description(), resource.dueDate());
    }
}
