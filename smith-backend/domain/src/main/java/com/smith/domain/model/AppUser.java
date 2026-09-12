package com.smith.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String passwordHash,
        Instant createdAt) {

    public AppUser {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
    }
}
