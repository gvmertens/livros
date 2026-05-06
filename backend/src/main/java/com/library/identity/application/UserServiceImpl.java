package com.library.identity.application;

import com.library.identity.application.dto.UserResponse;
import com.library.identity.domain.Role;
import com.library.identity.domain.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    @Inject
    UserRepository userRepository;

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findByIdOrThrow(userId);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void updateRole(UUID targetUserId, Role newRole) {
        User user = userRepository.findByIdOrThrow(targetUserId);
        user.role = newRole;
        userRepository.persist(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.id, user.name, user.email, user.role.name(), user.createdAt);
    }
}
