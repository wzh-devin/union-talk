
DROP TABLE IF EXISTS public.ut_friend_group;
CREATE TABLE public.ut_friend_group(
                                       id INT8,
                                       user_id INT8 NOT NULL,
                                       name VARCHAR(64) NOT NULL,
                                       sort_order INT4 NOT NULL DEFAULT 0,
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_friend_group.id IS '主键id';
COMMENT ON COLUMN public.ut_friend_group.user_id IS '用户id';
COMMENT ON COLUMN public.ut_friend_group.name IS '分组名称';
COMMENT ON COLUMN public.ut_friend_group.sort_order IS '分组排序';
COMMENT ON COLUMN public.ut_friend_group.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_friend_group.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_friend_group IS '好友分组';
DROP TABLE IF EXISTS public.ut_friend_request;
CREATE TABLE public.ut_friend_request(
                                         id INT8,
                                         from_user_id INT8 NOT NULL,
                                         to_user_id INT8 NOT NULL,
                                         apply_msg VARCHAR(255),
                                         status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                         handled_at TIMESTAMP,
                                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                         updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                         PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_friend_request.id IS '主键id';
COMMENT ON COLUMN public.ut_friend_request.from_user_id IS '申请用户';
COMMENT ON COLUMN public.ut_friend_request.to_user_id IS '目标用户';
COMMENT ON COLUMN public.ut_friend_request.apply_msg IS '申请信息';
COMMENT ON COLUMN public.ut_friend_request.status IS '申请状态';
COMMENT ON COLUMN public.ut_friend_request.handled_at IS '执行时间';
COMMENT ON COLUMN public.ut_friend_request.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_friend_request.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_friend_request IS '好友申请';
DROP TABLE IF EXISTS public.ut_user_relation;
CREATE TABLE public.ut_user_relation(
                                        id INT8,
                                        user_id INT8 NOT NULL,
                                        target_id INT8 NOT NULL,
                                        status VARCHAR(16) NOT NULL DEFAULT 'FRIEND',
                                        remark VARCHAR(64) NOT NULL,
                                        friend_group_id INT8 NOT NULL,
                                        deleted_at TIMESTAMP,
                                        blocked_at TIMESTAMP,
                                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                        updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                        PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_user_relation.id IS '主键id';
COMMENT ON COLUMN public.ut_user_relation.user_id IS '用户id';
COMMENT ON COLUMN public.ut_user_relation.target_id IS '对方id';
COMMENT ON COLUMN public.ut_user_relation.status IS '关系状态';
COMMENT ON COLUMN public.ut_user_relation.remark IS '备注';
COMMENT ON COLUMN public.ut_user_relation.friend_group_id IS '对方所在分组';
COMMENT ON COLUMN public.ut_user_relation.deleted_at IS '删除时间';
COMMENT ON COLUMN public.ut_user_relation.blocked_at IS '拉黑时间';
COMMENT ON COLUMN public.ut_user_relation.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_user_relation.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_user_relation IS '用户关系';
DROP TABLE IF EXISTS public.ut_group;
CREATE TABLE public.ut_group(
                                id INT8,
                                owner_id INT8 NOT NULL,
                                name VARCHAR(100) NOT NULL,
                                avatar_url TEXT,
                                description VARCHAR(255),
                                member_limit INT NOT NULL DEFAULT 100,
                                status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                                dissolved_at VARCHAR,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_group.id IS '主键id';
COMMENT ON COLUMN public.ut_group.owner_id IS '群主id';
COMMENT ON COLUMN public.ut_group.name IS '群聊名称';
COMMENT ON COLUMN public.ut_group.avatar_url IS '群聊头像';
COMMENT ON COLUMN public.ut_group.description IS '群聊描述';
COMMENT ON COLUMN public.ut_group.member_limit IS '人员限制';
COMMENT ON COLUMN public.ut_group.status IS '群聊状态';
COMMENT ON COLUMN public.ut_group.dissolved_at IS '解散时间';
COMMENT ON COLUMN public.ut_group.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_group.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_group IS '群聊';
DROP TABLE IF EXISTS public.ut_group_member;
CREATE TABLE public.ut_group_member(
                                       id INT8,
                                       group_id INT8 NOT NULL,
                                       user_id INT8 NOT NULL,
                                       role VARCHAR(16) NOT NULL,
                                       nickname VARCHAR(64) NOT NULL,
                                       joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       left_at TIMESTAMP,
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_group_member.id IS '主键id';
COMMENT ON COLUMN public.ut_group_member.group_id IS '群组id';
COMMENT ON COLUMN public.ut_group_member.user_id IS '用户id';
COMMENT ON COLUMN public.ut_group_member.role IS '用户角色';
COMMENT ON COLUMN public.ut_group_member.nickname IS '群昵称';
COMMENT ON COLUMN public.ut_group_member.joined_at IS '加入时间';
COMMENT ON COLUMN public.ut_group_member.left_at IS '离开时间';
COMMENT ON COLUMN public.ut_group_member.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_group_member.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_group_member IS '群成员';