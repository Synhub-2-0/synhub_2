package com.hub.synhub_backend.groups.interfaces.rest.resources;

import com.hub.synhub_backend.iam.interfaces.rest.resources.PublicUserResource;

public record InvitationResource(Long id, GroupResourceReduced group, PublicUserResource user) {}
