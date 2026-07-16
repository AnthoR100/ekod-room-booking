CREATE TABLE conversations
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    conversation_id    BIGINT       NOT NULL REFERENCES conversations   (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()


);

CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_chat_messages_conversation_id ON chat_messages(conversation_id);