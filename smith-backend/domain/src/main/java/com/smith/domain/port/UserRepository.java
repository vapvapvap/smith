package com.smith.domain.port;

import com.smith.domain.model.AppUser;

import java.util.Optional;

public interface UserRepository {

    Optional<AppUser> findByUsername(String username);
}
