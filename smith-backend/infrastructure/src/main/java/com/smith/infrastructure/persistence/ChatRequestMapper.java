package com.smith.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatRequestMapper {

    @Insert("""
            INSERT INTO chat_request
              (id, prompt, system_prompt, model, content, reasoning_content, finish_reason,
               prompt_tokens, completion_tokens, total_tokens,
               streamed, status, error_message, created_at)
            VALUES
              (CAST(#{id} AS uuid), #{prompt}, #{systemPrompt}, #{model}, #{content}, #{reasoningContent}, #{finishReason},
               #{promptTokens}, #{completionTokens}, #{totalTokens},
               #{streamed}, #{status}, #{errorMessage}, #{createdAt})
            """)
    void insert(ChatRecordPo po);

    @Select("""
            SELECT id, prompt, system_prompt, model, content, reasoning_content, finish_reason,
                   prompt_tokens, completion_tokens, total_tokens,
                   streamed, status, error_message, created_at
            FROM chat_request
            WHERE id = CAST(#{id} AS uuid)
            """)
    ChatRecordPo findById(@Param("id") String id);
}
