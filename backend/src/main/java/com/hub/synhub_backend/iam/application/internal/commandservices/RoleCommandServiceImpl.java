package com.hub.synhub_backend.iam.application.internal.commandservices;

import com.hub.synhub_backend.iam.domain.model.commands.SeedRolesCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.valueobjects.Roles;
import com.hub.synhub_backend.iam.domain.services.RoleCommandService;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Implementation of {@link RoleCommandService} to handle {@link SeedRolesCommand}
 */
@Service
public class RoleCommandServiceImpl implements RoleCommandService {

  private final RoleRepository roleRepository;

  public RoleCommandServiceImpl(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  /**
   * This method will handle the {@link SeedRolesCommand} and will create the roles if not exists
   * @param command {@link SeedRolesCommand}
   * @see SeedRolesCommand
   */
  @Override
  public void handle(SeedRolesCommand command) {
    Arrays.stream(Roles.values())
        .forEach(role -> {
          if(!roleRepository.existsByName(role)) {
            roleRepository.save(new Role(Roles.valueOf(role.name())));
          }
        } );
  }
}
