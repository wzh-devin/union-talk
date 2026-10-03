CREATE TABLE IF NOT EXISTS public.ut_message_mention
(
    id              BIGINT,
    message_id      BIGINT,
    conversation_id BIGINT,
    mention_type    VARCHAR(32),
    target_id       BIGINT,
    display_text    VARCHAR(128),
    start_offset    INTEGER,
    length          INTEGER,
    created_at      TIMESTAMP
);

COMMENT ON TABLE public.ut_message_mention IS '消息提及信息表';
COMMENT ON COLUMN public.ut_message_mention.id IS '提及记录id';
COMMENT ON COLUMN public.ut_message_mention.message_id IS '消息id';
COMMENT ON COLUMN public.ut_message_mention.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_message_mention.mention_type IS '提及类型';
COMMENT ON COLUMN public.ut_message_mention.target_id IS '提及目标id';
COMMENT ON COLUMN public.ut_message_mention.display_text IS '正文展示文本';
COMMENT ON COLUMN public.ut_message_mention.start_offset IS '正文起始位置';
COMMENT ON COLUMN public.ut_message_mention.length IS '正文文本长度';
COMMENT ON COLUMN public.ut_message_mention.created_at IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_ut_message_mention_message_id
    ON public.ut_message_mention (message_id);
CREATE INDEX IF NOT EXISTS idx_ut_message_mention_target
    ON public.ut_message_mention (mention_type, target_id, message_id);
CREATE INDEX IF NOT EXISTS idx_ut_message_mention_conversation
    ON public.ut_message_mention (conversation_id, message_id);

ALTER TABLE public.ut_message
    DROP COLUMN IF EXISTS is_at_ai,
    DROP COLUMN IF EXISTS at_uid_list,
    DROP COLUMN IF EXISTS is_at_all;
