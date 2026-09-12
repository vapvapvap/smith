package com.smith.infrastructure.persistence;

import com.smith.domain.model.AppUser;
import com.smith.domain.port.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class PersistenceUserRepository implements UserRepository {

    private final UserMapper mapper;

    public PersistenceUserRepository(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return Optional.ofNullable(mapper.findByUsername(username)).map(this::toDomain);
    }

    private AppUser toDomain(AppUserPo po) {
        return new AppUser(
                UUID.fromString(po.getId()),
                po.getUsername(),
                po.getPasswordHash(),
                po.getCreatedAt().toInstant());
    }
}
