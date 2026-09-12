CREATE TABLE chat_request (
  id UUID PRIMARY KEY,
  prompt TEXT NOT NULL,
  model VARCHAR(64) NOT NULL,
  content TEXT,
  reasoning_content TEXT,
  finish_reason VARCHAR(32),
  prompt_tokens INT,
  completion_tokens INT,
  total_tokens INT,
  streamed BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(32) NOT NULL,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
