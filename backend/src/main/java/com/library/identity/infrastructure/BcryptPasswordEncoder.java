package com.library.identity.infrastructure;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * BCrypt password encoder with cost factor 12.
 * Delegates to Quarkus's built-in {@link BcryptUtil}.
 */
@ApplicationScoped
public class BcryptPasswordEncoder {

    private static final int BCRYPT_COST = 12;

    /**
     * Hashes a plaintext password using BCrypt cost 12.
     *
     * @param plaintext the raw password
     * @return BCrypt hash string
     */
    public String encode(String plaintext) {
        return BcryptUtil.bcryptHash(plaintext, BCRYPT_COST);
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     *
     * @param plaintext the raw password to verify
     * @param hash      the stored BCrypt hash
     * @return true if the password matches
     */
    public boolean matches(String plaintext, String hash) {
        return BcryptUtil.matches(plaintext, hash);
    }
}
