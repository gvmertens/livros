package com.library.identity.application;

import com.library.identity.application.dto.ProfileResponse;
import com.library.identity.application.dto.ProfileUpdateRequest;
import com.library.identity.domain.Profile;
import com.library.shared.exception.ForbiddenException;
import com.library.shared.exception.NotFoundException;
import com.library.shared.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ProfileServiceImpl implements ProfileService {

    @Inject
    ProfileRepository profileRepository;

    @Override
    public ProfileResponse getProfile(UUID requestingUserId, UUID profileOwnerId) {
        if (!requestingUserId.equals(profileOwnerId)) {
            throw ForbiddenException.accessDenied();
        }

        Profile profile = profileRepository.findByUserId(profileOwnerId)
                .orElseThrow(() -> NotFoundException.of("Profile", profileOwnerId));

        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        if (request.displayName() != null && request.displayName().length() > 100) {
            throw new ValidationException("Display name must be 100 characters or fewer");
        }

        if (request.bio() != null && request.bio().length() > 1000) {
            throw new ValidationException("Bio must be 1000 characters or fewer");
        }

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> NotFoundException.of("Profile", userId));

        profile.displayName = request.displayName();
        profile.bio = request.bio();
        profile.favoriteGenres = request.favoriteGenres();
        profileRepository.persist(profile);

        return toProfileResponse(profile);
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        return new ProfileResponse(
                profile.id,
                profile.userId,
                profile.displayName,
                profile.bio,
                profile.favoriteGenres,
                profile.createdAt,
                profile.updatedAt);
    }
}
