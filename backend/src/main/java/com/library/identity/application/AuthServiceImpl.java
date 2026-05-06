package com.library.identity.application;

import com.library.identity.application.dto.LoginRequest;
import com.library.identity.application.dto.LoginResponse;
import com.library.identity.application.dto.RegisterRequest;
import com.library.identity.application.dto.UserResponse;
import com.library.identity.application.event.UserCreatedPayload;
import com.library.identity.domain.Profile;
import com.library.identity.domain.Role;
import com.library.identity.domain.User;
import com.library.identity.infrastructure.BcryptPasswordEncoder;
import com.library.identity.infrastructure.JwtIssuer;
import com.library.shared.event.DomainEventEnvelope;
import com.library.shared.event.EventBus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.UnauthorizedException;
import com.library.shared.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    ProfileRepository profileRepository;

    @Inject
    BcryptPasswordEncoder passwordEncoder;

    @Inject
    JwtIssuer jwtIssuer;

    @Inject
    EventBus eventBus;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }

        User user = new User();
        user.name = request.name();
        user.email = request.email();
        user.passwordHash = passwordEncoder.encode(request.password());
        user.role = Role.USER;
        userRepository.persist(user);

        Profile profile = new Profile();
        profile.userId = user.id;
        profile.displayName = null;
        profile.bio = null;
        profile.favoriteGenres = null;
        profileRepository.persist(profile);

        UserCreatedPayload payload = new UserCreatedPayload(user.id, user.email, user.createdAt);
        eventBus.publish(new DomainEventEnvelope(
                UUID.randomUUID(),
                "user.created",
                Instant.now(),
                payload));

        return toUserResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(UnauthorizedException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.passwordHash)) {
            throw UnauthorizedException.invalidCredentials();
        }

        String token = jwtIssuer.issue(user);
        return new LoginResponse(token, 3600L);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.id, user.name, user.email, user.role.name(), user.createdAt);
    }
}
