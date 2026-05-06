package com.library.identity.application;

import com.library.identity.application.dto.ProfileResponse;
import com.library.identity.application.dto.ProfileUpdateRequest;

import java.util.UUID;

public interface ProfileService {

    ProfileResponse getProfile(UUID requestingUserId, UUID profileOwnerId);

    ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);
}
