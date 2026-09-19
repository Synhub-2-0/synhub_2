package com.hub.synhub_backend.iam.interfaces.rest;

import com.hub.synhub_backend.iam.domain.model.queries.GetUserByUsernameQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.iam.domain.model.queries.GetAllUsersQuery;
import com.hub.synhub_backend.iam.domain.model.queries.GetUserByIdQuery;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;
import com.hub.synhub_backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * This class is a REST controller that exposes the users resource.
 * It includes the following operations:
 * - GET /api/v1/users: returns all the users
 * - GET /api/v1/users/{userId}: returns the user with the given id
 **/
@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User Management Endpoints")
public class UsersController {

  private final UserQueryService userQueryService;


  public UsersController(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  /**
   * This method returns all the users.
   *
   * @return a list of user resources.
   * @see UserResource
   */
  @GetMapping
  public ResponseEntity<List<UserResource>> getAllUsers() {
    var getAllUsersQuery = new GetAllUsersQuery();
    var users = userQueryService.handle(getAllUsersQuery);
    var userResources = users.stream()
        .map(UserResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
    return ResponseEntity.ok(userResources);
  }

  /**
   * This method returns the user with the given id.
   *
   * @param userId the user id.
   * @return the user resource with the given id
   * @throws RuntimeException if the user is not found
   * @see UserResource
   */
  @GetMapping(value = "/{userId}")
  public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
    var getUserByIdQuery = new GetUserByIdQuery(userId);
    var user = userQueryService.handle(getUserByIdQuery);
    if (user.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
    return ResponseEntity.ok(userResource);
  }

  @GetMapping("/username")
  @Operation(summary = "Get user by username", description = "Get user by username")
  public ResponseEntity<UserResource> getUserByUsername(@RequestParam String username) {
    var getUserByUsernameQuery = new GetUserByUsernameQuery(username);
    var userOpt = this.userQueryService.handle(getUserByUsernameQuery);
    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var user = userOpt.get();
    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user);
    return ResponseEntity.ok(userResource);
  }
}
