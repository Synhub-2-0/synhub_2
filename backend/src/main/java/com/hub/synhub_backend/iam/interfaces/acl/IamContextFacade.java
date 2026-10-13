package com.hub.synhub_backend.iam.interfaces.acl;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

/**
 * IamContextFacade
 * <p>
 *     This class is a facade for the IAM context. It provides a simple interface for other
 *     bounded contexts to interact with the
 *     IAM context.
 *     This class is a part of the ACL layer.
 * </p>
 *
 */
public interface IamContextFacade {

  Long createUser(String username, String name, String surname, String imgUrl, String email, String password);
  Long createUser(String username, String name, String surname, String imgUrl, String email, String password, List<String> roleNames);
  Long fetchUserIdByUsername(String username);
  String fetchUsernameByUserId(Long userId);
  Optional<User> fetchUserById(Long userId);
  List<User> fetchUsersByGroupId(Long groupId);
}
