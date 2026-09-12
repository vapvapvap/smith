package com.smith.domain.port;

import com.smith.domain.model.ChatRecord;

import java.util.Optional;
import java.util.UUID;

public interface ChatRequestRepository {

    void save(ChatRecord record);

    Optional<ChatRecord> findById(UUID id);
}
