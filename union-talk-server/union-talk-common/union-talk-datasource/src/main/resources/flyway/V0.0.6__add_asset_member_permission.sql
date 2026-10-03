ALTER TABLE public.ut_asset_folder
    ADD COLUMN folder_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN owner_user_id INT8;

COMMENT ON COLUMN public.ut_asset_folder.folder_type IS '目录类型';
COMMENT ON COLUMN public.ut_asset_folder.owner_user_id IS '成员空间所有者用户id';

ALTER TABLE public.ut_asset_folder
    ADD CONSTRAINT ck_asset_folder_type_owner
        CHECK (
            (folder_type = 'MEMBER_ROOT' AND owner_user_id IS NULL)
            OR (folder_type = 'MEMBER_HOME' AND owner_user_id IS NOT NULL)
            OR folder_type = 'NORMAL'
        );

CREATE UNIQUE INDEX uk_asset_folder_member_root
    ON public.ut_asset_folder(conversation_id)
    WHERE folder_type = 'MEMBER_ROOT' AND status = 'NORMAL';

CREATE UNIQUE INDEX uk_asset_folder_member_home
    ON public.ut_asset_folder(conversation_id, owner_user_id)
    WHERE folder_type = 'MEMBER_HOME' AND status = 'NORMAL';

CREATE INDEX idx_asset_folder_conversation_owner
    ON public.ut_asset_folder(conversation_id, owner_user_id)
    WHERE status = 'NORMAL';
