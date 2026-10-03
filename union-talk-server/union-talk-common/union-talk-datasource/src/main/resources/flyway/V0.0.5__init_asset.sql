CREATE TABLE public.ut_asset_folder(
                                       id INT8,
                                       conversation_id INT8 NOT NULL,
                                       parent_id INT8 NOT NULL DEFAULT 0,
                                       name VARCHAR(100) NOT NULL,
                                       path_ids VARCHAR(2000) NOT NULL,
                                       level_no INT4 NOT NULL DEFAULT 0,
                                       sort_no INT4 NOT NULL DEFAULT 0,
                                       status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
                                       deleted_at TIMESTAMPTZ,
                                       created_by INT8 NOT NULL,
                                       updated_by INT8 NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_asset_folder.id IS '主键id';
COMMENT ON COLUMN public.ut_asset_folder.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_asset_folder.parent_id IS '父级目录id';
COMMENT ON COLUMN public.ut_asset_folder.name IS '目录名称';
COMMENT ON COLUMN public.ut_asset_folder.path_ids IS '文件目录id路径';
COMMENT ON COLUMN public.ut_asset_folder.level_no IS '目录层级';
COMMENT ON COLUMN public.ut_asset_folder.sort_no IS '排序';
COMMENT ON COLUMN public.ut_asset_folder.status IS '目录状态';
COMMENT ON COLUMN public.ut_asset_folder.deleted_at IS '删除时间';
COMMENT ON COLUMN public.ut_asset_folder.created_by IS '创建人';
COMMENT ON COLUMN public.ut_asset_folder.updated_by IS '更新人';
COMMENT ON COLUMN public.ut_asset_folder.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_asset_folder.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_asset_folder IS '资产目录';

CREATE TABLE public.ut_asset_file(
                                     id INT8,
                                     conversation_id INT8 NOT NULL,
                                     folder_id INT8 NOT NULL,
                                     name VARCHAR(255) NOT NULL,
                                     file_ext VARCHAR(30) NOT NULL DEFAULT '',
                                     file_type VARCHAR(50) NOT NULL,
                                     file_size INT8 NOT NULL DEFAULT 0,
                                     storage_type VARCHAR(16) NOT NULL DEFAULT 'MINIO',
                                     storage_key VARCHAR(1000) NOT NULL,
                                     bucket_name VARCHAR(100) NOT NULL DEFAULT 'union-talk',
                                     mime_type VARCHAR(100) NOT NULL,
                                     sha256 CHAR(64),
                                     status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
                                     metadata_json TEXT NOT NULL DEFAULT '{}',
                                     etag VARCHAR(128),
                                     deleted_at TIMESTAMPTZ,
                                     created_by INT8 NOT NULL,
                                     updated_by INT8 NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_asset_file.id IS '主键id';
COMMENT ON COLUMN public.ut_asset_file.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_asset_file.folder_id IS '文件夹id';
COMMENT ON COLUMN public.ut_asset_file.name IS '文件名称';
COMMENT ON COLUMN public.ut_asset_file.file_ext IS '文件扩展名';
COMMENT ON COLUMN public.ut_asset_file.file_type IS '文件类型';
COMMENT ON COLUMN public.ut_asset_file.file_size IS '文件大小';
COMMENT ON COLUMN public.ut_asset_file.storage_type IS '存储类型';
COMMENT ON COLUMN public.ut_asset_file.storage_key IS '存储Key';
COMMENT ON COLUMN public.ut_asset_file.bucket_name IS '存储桶';
COMMENT ON COLUMN public.ut_asset_file.mime_type IS 'mime类型';
COMMENT ON COLUMN public.ut_asset_file.sha256 IS 'SHA256';
COMMENT ON COLUMN public.ut_asset_file.status IS '文件状态';
COMMENT ON COLUMN public.ut_asset_file.metadata_json IS '元数据JSON';
COMMENT ON COLUMN public.ut_asset_file.etag IS '对象存储ETag';
COMMENT ON COLUMN public.ut_asset_file.deleted_at IS '删除时间';
COMMENT ON COLUMN public.ut_asset_file.created_by IS '创建人';
COMMENT ON COLUMN public.ut_asset_file.updated_by IS '更新人';
COMMENT ON COLUMN public.ut_asset_file.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_asset_file.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_asset_file IS '资产文件';

CREATE TABLE public.ut_upload_session(
                                         id INT8,
                                         upload_token VARCHAR(100) NOT NULL,
                                         conversation_id INT8 NOT NULL,
                                         folder_id INT8 NOT NULL,
                                         asset_id INT8,
                                         file_name VARCHAR(255) NOT NULL,
                                         file_ext VARCHAR(30) NOT NULL DEFAULT '',
                                         mime_type VARCHAR(100) NOT NULL,
                                         file_size INT8 NOT NULL,
                                         file_sha256 CHAR(64),
                                         chunk_size INT8 NOT NULL DEFAULT 0,
                                         chunk_count INT4 NOT NULL DEFAULT 0,
                                         uploaded_chunk_count INT4 NOT NULL DEFAULT 0,
                                         bucket_name VARCHAR(100) NOT NULL DEFAULT 'union-talk',
                                         temp_storage_key VARCHAR(1000) NOT NULL,
                                         status VARCHAR(16) NOT NULL DEFAULT 'UPLOADING',
                                         failure_reason VARCHAR(255),
                                         paused_at TIMESTAMPTZ,
                                         canceled_at TIMESTAMPTZ,
                                         completed_at TIMESTAMPTZ,
                                         created_by INT8 NOT NULL,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_upload_session.id IS '主键id';
COMMENT ON COLUMN public.ut_upload_session.upload_token IS '上传token';
COMMENT ON COLUMN public.ut_upload_session.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_upload_session.folder_id IS '文件夹id';
COMMENT ON COLUMN public.ut_upload_session.asset_id IS '上传完成后的资产文件id';
COMMENT ON COLUMN public.ut_upload_session.file_name IS '文件名称';
COMMENT ON COLUMN public.ut_upload_session.file_ext IS '文件扩展名';
COMMENT ON COLUMN public.ut_upload_session.mime_type IS 'mime类型';
COMMENT ON COLUMN public.ut_upload_session.file_size IS '文件大小';
COMMENT ON COLUMN public.ut_upload_session.file_sha256 IS '文件SHA256';
COMMENT ON COLUMN public.ut_upload_session.chunk_size IS '分片大小';
COMMENT ON COLUMN public.ut_upload_session.chunk_count IS '分片数量';
COMMENT ON COLUMN public.ut_upload_session.uploaded_chunk_count IS '已经上传的分片数量';
COMMENT ON COLUMN public.ut_upload_session.bucket_name IS '存储桶名称';
COMMENT ON COLUMN public.ut_upload_session.temp_storage_key IS '临时文件key前缀';
COMMENT ON COLUMN public.ut_upload_session.status IS '上传状态';
COMMENT ON COLUMN public.ut_upload_session.failure_reason IS '失败原因';
COMMENT ON COLUMN public.ut_upload_session.paused_at IS '暂停时间';
COMMENT ON COLUMN public.ut_upload_session.canceled_at IS '取消时间';
COMMENT ON COLUMN public.ut_upload_session.completed_at IS '完成时间';
COMMENT ON COLUMN public.ut_upload_session.created_by IS '创建人';
COMMENT ON COLUMN public.ut_upload_session.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_upload_session.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_upload_session IS '上传任务表';

CREATE TABLE public.ut_upload_chunk(
                                       id INT8,
                                       session_id INT8 NOT NULL,
                                       chunk_index INT4 NOT NULL,
                                       chunk_size INT8 NOT NULL,
                                       chunk_sha256 CHAR(64) NOT NULL,
                                       storage_key VARCHAR(1000) NOT NULL,
                                       etag VARCHAR(128),
                                       status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
                                       uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_upload_chunk.id IS '主键id';
COMMENT ON COLUMN public.ut_upload_chunk.session_id IS '任务id';
COMMENT ON COLUMN public.ut_upload_chunk.chunk_index IS '分片序号';
COMMENT ON COLUMN public.ut_upload_chunk.chunk_size IS '分片大小';
COMMENT ON COLUMN public.ut_upload_chunk.chunk_sha256 IS '分片SHA256';
COMMENT ON COLUMN public.ut_upload_chunk.storage_key IS '临时分片对象Key';
COMMENT ON COLUMN public.ut_upload_chunk.etag IS '对象存储ETag';
COMMENT ON COLUMN public.ut_upload_chunk.status IS '状态';
COMMENT ON COLUMN public.ut_upload_chunk.uploaded_at IS '上传时间';
COMMENT ON TABLE public.ut_upload_chunk IS '上传分片表';

CREATE UNIQUE INDEX uk_asset_folder_parent_name
    ON public.ut_asset_folder(conversation_id, parent_id, name)
    WHERE status = 'NORMAL';
CREATE INDEX idx_asset_folder_conversation_path
    ON public.ut_asset_folder(conversation_id, path_ids);
CREATE UNIQUE INDEX uk_asset_file_folder_name
    ON public.ut_asset_file(conversation_id, folder_id, name)
    WHERE status = 'NORMAL';
CREATE INDEX idx_asset_file_folder
    ON public.ut_asset_file(conversation_id, folder_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX uk_upload_session_token
    ON public.ut_upload_session(upload_token);
CREATE INDEX idx_upload_session_user_status
    ON public.ut_upload_session(created_by, status, created_at DESC, id DESC);
CREATE UNIQUE INDEX uk_upload_chunk_session_index
    ON public.ut_upload_chunk(session_id, chunk_index);
