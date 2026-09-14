package com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * This interface is responsible for providing the User entity related operations.
 * It extends the JpaRepository interface.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
  /**
   * This method is responsible for finding the user by username.
   * @param username The username.
   * @return The user object.
   */
  Optional<User> findByUsername(String username);

  /**
   * This method is responsible for checking if the user exists by username.
   * @param username The username.
   * @return True if the user exists, false otherwise.
   */
  boolean existsByUsername(String username);

  /**
   * This method is responsible for checking if the user exists by email.
   * @param email The email.
   * @return True if the user exists, false otherwise.
   */
  boolean existsByEmail(String email);

  /**
   * This method is responsible for finding the user by member ID.
   * @param memberId The member ID.
   * @return The user object.
   */
  Optional<User> findByMember_Id(Long memberId);

  /**
   * This method is responsible for finding the user by leader ID.
   * @param leaderId The leader ID.
   * @return The user object.
   */
  Optional<User> findByLeader_Id(Long leaderId);

  List<User> findByMember_Group_Id(Long groupId);
}
