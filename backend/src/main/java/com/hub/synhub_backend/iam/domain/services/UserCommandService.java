package com.hub.synhub_backend.iam.domain.services;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.commands.CreateUserLeaderCommand;
import com.hub.synhub_backend.iam.domain.model.commands.CreateUserMemberCommand;
import com.hub.synhub_backend.iam.domain.model.commands.SignInCommand;
import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;

public interface UserCommandService {
  /**
   * Handle sign in command
   * This method handles the sign in command and returns the user and the token
   * @param command
   * @return user and token
   */
  Optional<ImmutablePair<User, String>> handle(SignInCommand command);
  /**
   * Handle sign up command
   * This method handles the sign-up command and returns the user
   * @param command
   * @return user
   */
  Optional<User> handle(SignUpCommand command);

    /**
     * Handle create user leader command
     * This method handles the create user leader command and returns the user
     * @param command
     * @return user
     */
  Optional<User> handle(CreateUserLeaderCommand command);

    /**
     * Handle create user member command
     * This method handles the create user member command and returns the user
     * @param command
     * @return user
     */
  Optional<User> handle(CreateUserMemberCommand command);
}
