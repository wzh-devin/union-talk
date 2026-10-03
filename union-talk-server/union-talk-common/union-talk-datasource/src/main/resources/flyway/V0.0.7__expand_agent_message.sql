ALTER TABLE public.ut_message
    ADD COLUMN sender_type VARCHAR(16) DEFAULT 'USER';

COMMENT ON COLUMN public.ut_message.sender_type IS '消息发送者类型';

ALTER TABLE public.ut_message
    ALTER COLUMN sender_id DROP NOT NULL;

COMMENT ON COLUMN public.ut_message.sender_id IS '发送者用户ID';

CREATE INDEX idx_ut_message_quote_msg_id
    ON public.ut_message (quote_msg_id);

CREATE INDEX idx_ut_message_conversation_time
    ON public.ut_message (conversation_id, created_at, id);

CREATE TABLE public.ut_message_agent_extension (
    message_id BIGINT,
    agent_run_id BIGINT,
    trigger_message_id BIGINT,
    agent_id BIGINT,
    model_id VARCHAR(128),
    citations_json JSONB DEFAULT '[]'::JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE public.ut_message_agent_extension IS 'AI消息扩展表';
COMMENT ON COLUMN public.ut_message_agent_extension.message_id IS 'AI回复消息ID';
COMMENT ON COLUMN public.ut_message_agent_extension.agent_run_id IS 'Agent运行ID';
COMMENT ON COLUMN public.ut_message_agent_extension.trigger_message_id IS '触发消息ID';
COMMENT ON COLUMN public.ut_message_agent_extension.agent_id IS '稳定Agent定义ID';
COMMENT ON COLUMN public.ut_message_agent_extension.model_id IS '模型标识';
COMMENT ON COLUMN public.ut_message_agent_extension.citations_json IS '引用信息';
COMMENT ON COLUMN public.ut_message_agent_extension.created_at IS '创建时间';

CREATE INDEX idx_ut_message_agent_extension_message
    ON public.ut_message_agent_extension (message_id);

CREATE INDEX idx_ut_message_agent_extension_run
    ON public.ut_message_agent_extension (agent_run_id);

CREATE INDEX idx_ut_message_agent_extension_trigger
    ON public.ut_message_agent_extension (trigger_message_id);

CREATE INDEX idx_ut_message_agent_extension_agent
    ON public.ut_message_agent_extension (agent_id);

CREATE TABLE public.ut_message_outbox (
    id BIGINT,
    event_id VARCHAR(64),
    aggregate_type VARCHAR(32),
    aggregate_id BIGINT,
    event_type VARCHAR(64),
    payload_json JSONB,
    status VARCHAR(16) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE public.ut_message_outbox IS '消息事件发件箱';
COMMENT ON COLUMN public.ut_message_outbox.id IS '发件箱记录ID';
COMMENT ON COLUMN public.ut_message_outbox.event_id IS '事件ID';
COMMENT ON COLUMN public.ut_message_outbox.aggregate_type IS '聚合类型';
COMMENT ON COLUMN public.ut_message_outbox.aggregate_id IS '聚合ID';
COMMENT ON COLUMN public.ut_message_outbox.event_type IS '事件类型';
COMMENT ON COLUMN public.ut_message_outbox.payload_json IS '事件内容';
COMMENT ON COLUMN public.ut_message_outbox.status IS '发布状态';
COMMENT ON COLUMN public.ut_message_outbox.retry_count IS '重试次数';
COMMENT ON COLUMN public.ut_message_outbox.next_retry_at IS '下次重试时间';
COMMENT ON COLUMN public.ut_message_outbox.published_at IS '发布时间';
COMMENT ON COLUMN public.ut_message_outbox.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_message_outbox.updated_at IS '更新时间';

CREATE INDEX idx_ut_message_outbox_id
    ON public.ut_message_outbox (id);

CREATE INDEX idx_ut_message_outbox_event
    ON public.ut_message_outbox (event_id);

CREATE INDEX idx_ut_message_outbox_publish
    ON public.ut_message_outbox (status, next_retry_at, created_at);

ALTER TABLE public.ut_user_conversation
    ADD COLUMN mention_unread_count INT DEFAULT 0;

COMMENT ON COLUMN public.ut_user_conversation.mention_unread_count IS '提及未读数';

ALTER TABLE public.ut_user_conversation
    ADD COLUMN last_mention_msg_id BIGINT;

COMMENT ON COLUMN public.ut_user_conversation.last_mention_msg_id IS '最后提及消息ID';

CREATE INDEX idx_ut_user_conversation_user_status
    ON public.ut_user_conversation (user_id, status, updated_at, id);

CREATE INDEX idx_ut_user_conversation_conversation_user
    ON public.ut_user_conversation (conversation_id, user_id);
