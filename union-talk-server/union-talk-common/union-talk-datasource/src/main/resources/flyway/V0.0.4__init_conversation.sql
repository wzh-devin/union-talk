
ALTER TABLE public.ut_user ALTER COLUMN last_login_at DROP default;
ALTER TABLE public.ut_user ALTER COLUMN last_login_at TYPE TIMESTAMPTZ USING last_login_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_user ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_user ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_user_device ALTER COLUMN last_active_at DROP default;
ALTER TABLE public.ut_user_device ALTER COLUMN last_active_at TYPE TIMESTAMPTZ USING last_active_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_device ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_user_device ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_device ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_user_device ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_user_setting ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_user_setting ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_setting ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_user_setting ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_friend_group ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_friend_group ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_friend_group ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_friend_group ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_friend_request ALTER COLUMN handled_at DROP default;
ALTER TABLE public.ut_friend_request ALTER COLUMN handled_at TYPE TIMESTAMPTZ USING handled_at::TIMESTAMPTZ;
ALTER TABLE public.ut_friend_request ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_friend_request ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_friend_request ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_friend_request ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_user_relation ALTER COLUMN deleted_at DROP default;
ALTER TABLE public.ut_user_relation ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_relation ALTER COLUMN blocked_at DROP default;
ALTER TABLE public.ut_user_relation ALTER COLUMN blocked_at TYPE TIMESTAMPTZ USING blocked_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_relation ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_user_relation ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_user_relation ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_user_relation ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_group ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_group ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_group ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_group ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

ALTER TABLE public.ut_group_member ALTER COLUMN joined_at DROP default;
ALTER TABLE public.ut_group_member ALTER COLUMN joined_at TYPE TIMESTAMPTZ USING joined_at::TIMESTAMPTZ;
ALTER TABLE public.ut_group_member ALTER COLUMN left_at DROP default;
ALTER TABLE public.ut_group_member ALTER COLUMN left_at TYPE TIMESTAMPTZ USING left_at::TIMESTAMPTZ;
ALTER TABLE public.ut_group_member ALTER COLUMN created_at DROP default;
ALTER TABLE public.ut_group_member ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::TIMESTAMPTZ;
ALTER TABLE public.ut_group_member ALTER COLUMN updated_at DROP default;
ALTER TABLE public.ut_group_member ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at::TIMESTAMPTZ;

DROP TABLE IF EXISTS public.ut_conversation;
CREATE TABLE public.ut_conversation(
                                       id INT8,
                                       type VARCHAR(16) NOT NULL,
                                       group_id INT8,
                                       last_msg_id INT8,
                                       last_msg_at TIMESTAMPTZ,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_conversation.id IS '主键id';
COMMENT ON COLUMN public.ut_conversation.type IS '会话类型';
COMMENT ON COLUMN public.ut_conversation.group_id IS '群聊id';
COMMENT ON COLUMN public.ut_conversation.last_msg_id IS '最后一条消息id';
COMMENT ON COLUMN public.ut_conversation.last_msg_at IS '最后一条消息时间';
COMMENT ON COLUMN public.ut_conversation.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_conversation.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_conversation IS '会话表';
DROP TABLE IF EXISTS public.ut_conversation_member;
CREATE TABLE public.ut_conversation_member(
                                              id INT8,
                                              conversation_id INT8 NOT NULL,
                                              user_id INT8 NOT NULL,
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                              updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                              PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_conversation_member.id IS '主键id';
COMMENT ON COLUMN public.ut_conversation_member.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_conversation_member.user_id IS '用户id';
COMMENT ON COLUMN public.ut_conversation_member.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_conversation_member.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_conversation_member IS '私聊会话成员';
DROP TABLE IF EXISTS public.ut_user_conversation;
CREATE TABLE public.ut_user_conversation(
                                            id INT8,
                                            user_id INT8,
                                            conversation_id INT8,
                                            unread_count INT DEFAULT 0,
                                            last_read_msg_id INT8,
                                            is_pinned BOOLEAN DEFAULT FALSE,
                                            is_muted BOOLEAN DEFAULT FALSE,
                                            status VARCHAR(16) DEFAULT 'ACTIVE',
                                            hidden_at TIMESTAMPTZ,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_user_conversation.id IS '主键id';
COMMENT ON COLUMN public.ut_user_conversation.user_id IS '用户id';
COMMENT ON COLUMN public.ut_user_conversation.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_user_conversation.unread_count IS '未读统计';
COMMENT ON COLUMN public.ut_user_conversation.last_read_msg_id IS '已读消息id';
COMMENT ON COLUMN public.ut_user_conversation.is_pinned IS '是否置顶';
COMMENT ON COLUMN public.ut_user_conversation.is_muted IS '是否免打扰';
COMMENT ON COLUMN public.ut_user_conversation.status IS '会话状态';
COMMENT ON COLUMN public.ut_user_conversation.hidden_at IS '会话删除时间';
COMMENT ON COLUMN public.ut_user_conversation.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_user_conversation.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_user_conversation IS '用户会话';
DROP TABLE IF EXISTS public.ut_message;
CREATE TABLE public.ut_message(
                                  id INT8,
                                  conversation_id INT8 NOT NULL,
                                  sender_id INT8 NOT NULL,
                                  type VARCHAR(16) NOT NULL,
                                  content TEXT NOT NULL DEFAULT '',
                                  quote_msg_id INT8,
                                  is_at_ai BOOLEAN NOT NULL DEFAULT FALSE,
                                  at_uid_list BIGINT[] NOT NULL DEFAULT '{}',
                                  is_at_all BOOLEAN DEFAULT FALSE,
                                  recalled BOOLEAN NOT NULL DEFAULT FALSE,
                                  recalled_at TIMESTAMPTZ,
                                  deleted_at TIMESTAMPTZ,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_message.id IS '主键id';
COMMENT ON COLUMN public.ut_message.conversation_id IS '会话id';
COMMENT ON COLUMN public.ut_message.sender_id IS '发送者id';
COMMENT ON COLUMN public.ut_message.type IS '消息类型';
COMMENT ON COLUMN public.ut_message.content IS '消息内容';
COMMENT ON COLUMN public.ut_message.quote_msg_id IS '引用的消息id';
COMMENT ON COLUMN public.ut_message.is_at_ai IS '是否@AI';
COMMENT ON COLUMN public.ut_message.at_uid_list IS '@群友id列表';
COMMENT ON COLUMN public.ut_message.is_at_all IS '是否@所有人';
COMMENT ON COLUMN public.ut_message.recalled IS '是否撤回';
COMMENT ON COLUMN public.ut_message.recalled_at IS '撤回时间';
COMMENT ON COLUMN public.ut_message.deleted_at IS '删除时间';
COMMENT ON COLUMN public.ut_message.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_message.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_message IS '消息表';
