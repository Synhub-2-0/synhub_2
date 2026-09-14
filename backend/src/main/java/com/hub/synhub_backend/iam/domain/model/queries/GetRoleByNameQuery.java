package com.hub.synhub_backend.iam.domain.model.queries;


import com.hub.synhub_backend.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles name) {
}
