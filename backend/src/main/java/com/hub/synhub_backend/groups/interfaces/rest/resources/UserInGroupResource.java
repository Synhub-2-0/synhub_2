package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.iam.interfaces.rest.resources.PublicUserResource;

public record UserInGroupResource(PublicUserResource user, GroupRole roleInGroup) {}
