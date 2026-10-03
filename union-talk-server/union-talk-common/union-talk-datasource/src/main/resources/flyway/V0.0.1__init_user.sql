
DROP TABLE IF EXISTS public.ut_user;
CREATE TABLE public.ut_user(
                               id INT8,
                               code CHAR(32) NOT NULL,
                               register_way VARCHAR(16) NOT NULL DEFAULT 'PASSWORD',
                               username VARCHAR(32) NOT NULL,
                               password VARCHAR(255) NOT NULL,
                               email VARCHAR(255) NOT NULL,
                               avatar_url TEXT,
                               bio VARCHAR(255),
                               need_friend_verify BOOLEAN NOT NULL DEFAULT TRUE,
                               status VARCHAR NOT NULL DEFAULT 'NORMAL',
                               last_login_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_user.id IS '主键id';
COMMENT ON COLUMN public.ut_user.code IS '用户唯一CODE';
COMMENT ON COLUMN public.ut_user.register_way IS '注册方式';
COMMENT ON COLUMN public.ut_user.username IS '用户名';
COMMENT ON COLUMN public.ut_user.password IS '密码';
COMMENT ON COLUMN public.ut_user.email IS '邮箱';
COMMENT ON COLUMN public.ut_user.avatar_url IS '头像地址';
COMMENT ON COLUMN public.ut_user.bio IS '简介';
COMMENT ON COLUMN public.ut_user.need_friend_verify IS '添加好友是否需要验证';
COMMENT ON COLUMN public.ut_user.status IS '用户账号状态';
COMMENT ON COLUMN public.ut_user.last_login_at IS '最近登录时间';
COMMENT ON COLUMN public.ut_user.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_user.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_user IS '用户表';


DROP TABLE IF EXISTS public.ut_user_device;
CREATE TABLE public.ut_user_device(
                                      id INT8,
                                      user_id INT8 NOT NULL,
                                      device_name VARCHAR(128) NOT NULL DEFAULT '',
                                      device_type VARCHAR(32) NOT NULL DEFAULT 'WEB',
                                      ip_address INET NOT NULL,
                                      user_agent TEXT NOT NULL DEFAULT '',
                                      last_active_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_user_device.id IS '主键id';
COMMENT ON COLUMN public.ut_user_device.user_id IS '用户id';
COMMENT ON COLUMN public.ut_user_device.device_name IS '设备名称';
COMMENT ON COLUMN public.ut_user_device.device_type IS '设备类型';
COMMENT ON COLUMN public.ut_user_device.ip_address IS 'IP地址';
COMMENT ON COLUMN public.ut_user_device.user_agent IS 'AGENT';
COMMENT ON COLUMN public.ut_user_device.last_active_at IS '最后的激活时间';
COMMENT ON COLUMN public.ut_user_device.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_user_device.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_user_device IS '用户登录设备';


DROP TABLE IF EXISTS public.ut_user_setting;
CREATE TABLE public.ut_user_setting(
                                       id INT8,
                                       user_id INT8 NOT NULL,
                                       key VARCHAR(64) NOT NULL,
                                       value TEXT NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       PRIMARY KEY (id)
);
COMMENT ON COLUMN public.ut_user_setting.id IS '主键id';
COMMENT ON COLUMN public.ut_user_setting.user_id IS '用户id';
COMMENT ON COLUMN public.ut_user_setting.key IS '配置Key';
COMMENT ON COLUMN public.ut_user_setting.value IS '配置Value';
COMMENT ON COLUMN public.ut_user_setting.created_at IS '创建时间';
COMMENT ON COLUMN public.ut_user_setting.updated_at IS '更新时间';
COMMENT ON TABLE public.ut_user_setting IS '用户设置';

