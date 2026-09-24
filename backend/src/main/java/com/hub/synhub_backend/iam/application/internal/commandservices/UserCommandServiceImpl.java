package com.hub.synhub_backend.iam.application.internal.commandservices;

import com.hub.synhub_backend.iam.application.internal.outboundservices.hashing.HashingService;
import com.hub.synhub_backend.iam.application.internal.outboundservices.tokens.TokenService;
import com.hub.synhub_backend.iam.domain.exceptions.EmailAlreadyExistsException;
import com.hub.synhub_backend.iam.domain.exceptions.UserOrPasswordInvalidException;
import com.hub.synhub_backend.iam.domain.exceptions.UsernameAlreadyExistsException;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.commands.SignInCommand;
import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.services.UserCommandService;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User command service implementation
 * <p>
 *     This class implements the {@link UserCommandService} interface and provides the implementation for the
 *     {@link SignInCommand} and {@link SignUpCommand} commands.
 * </p>
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

  private final UserRepository userRepository;
  private final HashingService hashingService;
  private final TokenService tokenService;

  private final RoleRepository roleRepository;

  public UserCommandServiceImpl(UserRepository userRepository, HashingService hashingService,
                                TokenService tokenService, RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.hashingService = hashingService;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
  }

  /**
   * Handle the sign-in command
   * <p>
   *     This method handles the {@link SignInCommand} command and returns the user and the token.
   * </p>
   * @param command the sign-in command containing the username and password
   * @return and optional containing the user matching the username and the generated token
   * @throws UserOrPasswordInvalidException if the user is not found or the password is invalid
   */
  @Override
  public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
    var user = userRepository.findByUsername(command.username());
    if (user.isEmpty() || !hashingService.matches(command.password(), user.get().getPassword()))
      throw new BadCredentialsException("User or password is incorrect");
    var token = tokenService.generateToken(user.get().getUsername());
    return Optional.of(ImmutablePair.of(user.get(), token));
  }

  /**
   * Handle the sign-up command
   * <p>
   *     This method handles the {@link SignUpCommand} command and returns the user.
   * </p>
   * @param command the sign-up command containing the username and password
   * @return the created user
   */
  @Override
  public Optional<User> handle(SignUpCommand command) {
    if (userRepository.existsByUsername(command.username()))
      throw new UsernameAlreadyExistsException("Username already exists");
    if(userRepository.existsByEmail(command.username()))
        throw new EmailAlreadyExistsException("User with this email already exists");
    var roles = command.roles().stream()
        .map(role ->
            roleRepository.findByName(role.getName())
                .orElseThrow(() -> new RuntimeException("Role name not found")))
        .toList();
    var user = new User(
            command.username(),
            command.name(),
            command.surname(),
            command.imgUrl(),
            command.email(),
            hashingService.encode(command.password()),
            roles);
    userRepository.save(user);
    return userRepository.findByUsername(command.username());
  }
}
