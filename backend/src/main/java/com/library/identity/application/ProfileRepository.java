package com.library.identity.application;

import com.library.identity.domain.Profile;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository {
    Optional<Profile> findByUserId(UUID userId);
    void persist(Profile profile);
}
