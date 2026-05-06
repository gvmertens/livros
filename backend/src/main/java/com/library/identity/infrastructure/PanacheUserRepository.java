package com.library.identity.infrastructure;

import com.library.identity.application.UserRepository;
import com.library.identity.domain.User;
import com.library.shared.exception.NotFoundException;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheUserRepository implements UserRepository, PanacheRepositoryBase<User, UUID> {

    @Override
    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    @Override
    public User findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Override
    public void persist(User user) {
        getEntityManager().persist(user);
    }
}
