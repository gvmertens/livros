package com.library.identity.infrastructure;

import com.library.identity.domain.User;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Set;

/**
 * Issues RS256-signed JWTs for authenticated users.
 *
 * <p>Token claims:
 * <ul>
 *   <li>{@code sub} — user UUID as string</li>
 *   <li>{@code email} — user email</li>
 *   <li>{@code groups} — singleton set containing the user's role (SmallRye role claim)</li>
 *   <li>{@code iss} — {@code personal-library-manager}</li>
 *   <li>{@code exp} — {@code iat + 3600} (1 hour)</li>
 * </ul>
 *
 * <p>The signing key is read from {@code smallrye.jwt.sign.key.location} in
 * {@code application.properties} (defaults to {@code META-INF/resources/privateKey.pem}).
 */
@ApplicationScoped
public class JwtIssuer {

    private static final String ISSUER = "personal-library-manager";
    private static final long EXPIRY_SECONDS = 3600L;

    /**
     * Issues a signed JWT for the given user.
     *
     * @param user the authenticated user
     * @return signed JWT string
     */
    public String issue(User user) {
        Instant now = Instant.now();
        return Jwt.issuer(ISSUER)
                .subject(user.id.toString())
                .claim("email", user.email)
                .groups(Set.of(user.role.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(EXPIRY_SECONDS))
                .sign();
    }
}
