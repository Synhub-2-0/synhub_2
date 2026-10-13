package com.hub.synhub_backend.iam.domain.services;


import com.hub.synhub_backend.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
  void handle(SeedRolesCommand command);
}
