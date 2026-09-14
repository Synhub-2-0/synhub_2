package com.hub.synhub_backend.iam.application.internal.acl;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.queries.*;
import com.hub.synhub_backend.iam.domain.services.UserCommandService;
import com.hub.synhub_backend.iam.domain.services.UserQueryService;
import com.hub.synhub_backend.iam.interfaces.acl.IamContextFacade;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class IamContextFacadeImpl implements IamContextFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public IamContextFacadeImpl(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    /**
     * Creates a user with the given username, password and default role.
     * @param username The username of the user.
     * @param name The name of the user.
     * @param surname The surname of the user.
     * @param imgUrl The image URL of the user.
     * @param email The email of the user.
     * @param password The password of the user.
     * @return The id of the created user.
     */
    public Long createUser(String username, String name, String surname, String imgUrl, String email, String password) {
        var signUpCommand = new SignUpCommand(
                username,
                name,
                surname,
                imgUrl,
                email,
                password,
                List.of(Role.getDefaultRole()));
        var result = userCommandService.handle(signUpCommand);
        if (result.isEmpty()) return 0L;
        return result.get().getId();
    }

    /**
     * Creates a user with the given username, password and roles.
     * @param username The username of the user.
     * @param name The name of the user.
     * @param surname The surname of the user.
     * @param imgUrl The image URL of the user.
     * @param email The email of the user.
     * @param password The password of the user.
     * @param roleNames The names of the roles to be assigned to the user.
     * @return The id of the created user.
     */
    public Long createUser(String username, String name, String surname, String imgUrl, String email, String password, List<String> roleNames) {
        var roles = roleNames != null
                ? roleNames.stream().map(Role::toRoleFromName).toList()
                : new ArrayList<Role>();
        var signUpCommand = new SignUpCommand(
                username,
                name,
                surname,
                imgUrl,
                email,
                password,
                roles);
        var result = userCommandService.handle(signUpCommand);
        if (result.isEmpty())
            return 0L;
        return result.get().getId();
    }

    /**
     * Fetches the id of the user with the given username.
     * @param username The username of the user.
     * @return The id of the user.
     */
    public Long fetchUserIdByUsername(String username) {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(username);
        var result = userQueryService.handle(getUserByUsernameQuery);
        if (result.isEmpty())
            return 0L;
        return result.get().getId();
    }

    /**
     * Fetches the username of the user with the given id.
     * @param userId The id of the user.
     * @return The username of the user.
     */
    public String fetchUsernameByUserId(Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var result = userQueryService.handle(getUserByIdQuery);
        if (result.isEmpty())
            return Strings.EMPTY;
        return result.get().getUsername();
    }

    /**
     * Fetches the user by its id.
     * @param memberId The id of the user.
     * @return An Optional containing the user if found, otherwise empty.
     */
    public Optional<User> fetchUserByMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            return Optional.empty();
        }
        var getUserByMemberIdQuery = new GetUserByMemberId(memberId);
        var result = userQueryService.handle(getUserByMemberIdQuery);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get());
    }

    public Optional<User> fetchUserByLeaderId(Long leaderId) {
        if (leaderId == null || leaderId <= 0) {
            return Optional.empty();
        }
        var getUserByLeaderIdQuery = new GetUserByLeaderId(leaderId);
        var result = userQueryService.handle(getUserByLeaderIdQuery);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get());
    }

    @Override
    public Optional<User> fetchUserById(Long userId) {
        if( userId == null || userId <= 0) {
            return Optional.empty();
        }
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var result = userQueryService.handle(getUserByIdQuery);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get());
    }

    @Override
    public List<User> fetchUsersByGroupId(Long groupId) {
        var getUserByGroupIdQuery = new GetUsersByGroupIdQuery(groupId);
        var result = userQueryService.handle(getUserByGroupIdQuery);
        return result;
    }
}
