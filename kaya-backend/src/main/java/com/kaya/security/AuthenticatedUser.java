package com.kaya.security;

/**
 * Principal stored in the SecurityContext after a valid access token is parsed.
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String username,
        Long sessionId
) {
}
