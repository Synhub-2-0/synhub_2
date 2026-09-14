package com.hub.synhub_backend.iam.application.internal.queryservices;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.queries.*;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link UserQueryService} interface.
 */
@Service
public class UserQueryServiceImpl implements UserQueryService {
  private final UserRepository userRepository;

  /**
   * Constructor.
   *
   * @param userRepository {@link UserRepository} instance.
   */
  public UserQueryServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * This method is used to handle {@link GetAllUsersQuery} query.
   * @param query {@link GetAllUsersQuery} instance.
   * @return {@link List} of {@link User} instances.
   * @see GetAllUsersQuery
   */
  @Override
  public List<User> handle(GetAllUsersQuery query) {
    return userRepository.findAll();
  }

  /**
   * This method is used to handle {@link GetUserByIdQuery} query.
   * @param query {@link GetUserByIdQuery} instance.
   * @return {@link Optional} of {@link User} instance.
   * @see GetUserByIdQuery
   */
  @Override
  public Optional<User> handle(GetUserByIdQuery query) {
    return userRepository.findById(query.userId());
  }

  /**
   * This method is used to handle {@link GetUserByUsernameQuery} query.
   * @param query {@link GetUserByUsernameQuery} instance.
   * @return {@link Optional} of {@link User} instance.
   * @see GetUserByUsernameQuery
   */
  @Override
  public Optional<User> handle(GetUserByUsernameQuery query) {
    return userRepository.findByUsername(query.username());
  }

  /**
   * This method is used to handle {@link GetUserByMemberId} query.
   * @param query {@link GetUserByMemberId} instance.
   * @return {@link Optional} of {@link User} instance.
   * @see GetUserByMemberId
   */
  @Override
  public Optional<User> handle(GetUserByMemberId query) {
    return userRepository.findByMember_Id(query.memberId());
  }

  /**
   * This method is used to handle {@link GetUserByLeaderId} query.
   * @param query {@link GetUserByLeaderId} instance.
   * @return {@link Optional} of {@link User} instance.
   * @see GetUserByLeaderId
   */
  @Override
  public Optional<User> handle(GetUserByLeaderId query) {
    return userRepository.findByLeader_Id(query.leaderId());
  }

  @Override
  public List<User> handle(GetUsersByGroupIdQuery query) {
    return userRepository.findByMember_Group_Id(query.groupId());
  }
}
