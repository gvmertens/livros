package com.library.identity.application;

import com.library.identity.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    User findByIdOrThrow(UUID id);
    void persist(User user);
}
