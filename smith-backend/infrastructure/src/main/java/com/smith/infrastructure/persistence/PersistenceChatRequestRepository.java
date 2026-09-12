package com.smith.infrastructure.persistence;

import com.smith.domain.model.ChatRecord;
import com.smith.domain.model.ChatStatus;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.ModelName;
import com.smith.domain.model.Prompt;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatRequestRepository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public class PersistenceChatRequestRepository implements ChatRequestRepository {

    private final ChatRequestMapper mapper;

    public PersistenceChatRequestRepository(ChatRequestMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ChatRecord record) {
        mapper.insert(toPo(record));
    }

    @Override
    public Optional<ChatRecord> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    private ChatRecordPo toPo(ChatRecord r) {
        ChatRecordPo po = new ChatRecordPo();
        po.setId(r.id().toString());
        po.setPrompt(r.prompt().value());
        po.setModel(r.model().value());
        po.setContent(r.content());
        po.setReasoningContent(r.reasoningContent());
        po.setFinishReason(r.finishReason() == null ? null : r.finishReason().name());
        if (r.usage() != null) {
            po.setPromptTokens(r.usage().promptTokens());
            po.setCompletionTokens(r.usage().completionTokens());
            po.setTotalTokens(r.usage().totalTokens());
        }
        po.setStreamed(r.streamed());
        po.setStatus(r.status().name());
        po.setErrorMessage(r.errorMessage());
        po.setCreatedAt(Timestamp.from(r.createdAt()));
        return po;
    }

    private ChatRecord toDomain(ChatRecordPo po) {
        return new ChatRecord(
                UUID.fromString(po.getId()),
                new Prompt(po.getPrompt()),
                new ModelName(po.getModel()),
                po.getContent(),
                po.getReasoningContent(),
                po.getFinishReason() == null ? null : FinishReason.valueOf(po.getFinishReason()),
                new Usage(po.getPromptTokens(), po.getCompletionTokens(), po.getTotalTokens()),
                po.isStreamed(),
                ChatStatus.valueOf(po.getStatus()),
                po.getErrorMessage(),
                po.getCreatedAt().toInstant());
    }
}
