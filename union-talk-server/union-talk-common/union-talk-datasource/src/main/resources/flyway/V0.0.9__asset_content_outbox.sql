ALTER TABLE public.ut_asset_file
    ADD COLUMN resource_version INTEGER DEFAULT 1;

COMMENT ON COLUMN public.ut_asset_file.resource_version IS '文件内容版本';

CREATE INDEX idx_ut_asset_file_content_version
    ON public.ut_asset_file (id, resource_version, status);

CREATE TABLE public.ut_asset_outbox
(
    id               BIGINT,
    event_id         VARCHAR(64),
    asset_file_id    BIGINT,
    resource_version INTEGER,
    event_type       VARCHAR(64),
    payload_json     JSONB,
    status           VARCHAR(16) DEFAULT 'PENDING',
    retry_count      INTEGER DEFAULT 0,
    next_retry_at    TIMESTAMPTZ,
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

COMMENT ON TABLE public.ut_asset_outbox IS '资产内容事件发件箱';
COMMENT ON COLUMN public.ut_asset_outbox.id IS '发件箱记录id';
COMMENT ON COLUMN public.ut_asset_outbox.event_id IS '事件id';
COMMENT ON COLUMN public.ut_asset_outbox.asset_file_id IS '资产文件id';
COMMENT ON COLUMN public.ut_asset_outbox.resource_version IS '文件内容版本';
COMMENT ON COLUMN public.ut_asset_outbox.event_type IS '事件类型';
COMMENT ON COLUMN public.ut_asset_outbox.payload_json IS '事件内容';
COMMENT ON COLUMN public.ut_asset_outbox.status IS '发布状态';
COMMENT ON COLUMN public.ut_asset_outbox.retry_count IS '重试次数';
COMMENT ON COLUMN public.ut_asset_outbox.next_retry_at IS '下次重试时间';
COMMENT ON COLUMN public.ut_asset_outbox.published_at IS '发布时间';
COMMENT ON COLUMN public.ut_asset_outbox.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_asset_outbox.updated_at IS '更新时间';

CREATE INDEX idx_ut_asset_outbox_id
    ON public.ut_asset_outbox (id);

CREATE INDEX idx_ut_asset_outbox_event
    ON public.ut_asset_outbox (event_id);

CREATE INDEX idx_ut_asset_outbox_asset_version
    ON public.ut_asset_outbox (asset_file_id, resource_version);

CREATE INDEX idx_ut_asset_outbox_publish
    ON public.ut_asset_outbox (status, next_retry_at, created_at);
