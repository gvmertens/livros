package com.library.identity.infrastructure;

import com.library.identity.application.ProfileRepository;
import com.library.identity.domain.Profile;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheProfileRepository implements ProfileRepository, PanacheRepositoryBase<Profile, UUID> {

    @Override
    public Optional<Profile> findByUserId(UUID userId) {
        return find("userId", userId).firstResultOptional();
    }

    @Override
    public void persist(Profile profile) {
        getEntityManager().persist(profile);
    }
}
