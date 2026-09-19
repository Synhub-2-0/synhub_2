package com.hub.synhub_backend.iam.domain.model.queries;


import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;

public record GetRoleByNameQuery(AccountRole name) {
}
