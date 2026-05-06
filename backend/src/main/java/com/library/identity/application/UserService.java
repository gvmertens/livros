package com.library.identity.application;

import com.library.identity.application.dto.UserResponse;
import com.library.identity.domain.Role;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);

    void updateRole(UUID targetUserId, Role newRole);
}
