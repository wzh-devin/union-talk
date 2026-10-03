-- ============================================================
-- Seata Server 2.x —— TC 全局事务存储表 (PostgreSQL)
-- ============================================================

-- 全局事务表
CREATE TABLE IF NOT EXISTS global_table
(
    xid                       VARCHAR(128) NOT NULL,
    transaction_id            BIGINT,
    status                    SMALLINT     NOT NULL,
    application_id            VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name          VARCHAR(128),
    timeout                   INT,
    begin_time                BIGINT,
    application_data          VARCHAR(2000),
    gmt_create                TIMESTAMP,
    gmt_modified              TIMESTAMP,
    PRIMARY KEY (xid)
    );
CREATE INDEX IF NOT EXISTS idx_global_status_gmt ON global_table (status, gmt_modified);
CREATE INDEX IF NOT EXISTS idx_global_transaction_id ON global_table (transaction_id);

-- 分支事务表
CREATE TABLE IF NOT EXISTS branch_table
(
    branch_id         BIGINT       NOT NULL,
    xid               VARCHAR(128) NOT NULL,
    transaction_id    BIGINT,
    resource_group_id VARCHAR(32),
    resource_id       VARCHAR(256),
    branch_type       VARCHAR(8),
    status            SMALLINT,
    client_id         VARCHAR(64),
    application_data  VARCHAR(2000),
    gmt_create        TIMESTAMP(6),
    gmt_modified      TIMESTAMP(6),
    PRIMARY KEY (branch_id)
    );
CREATE INDEX IF NOT EXISTS idx_branch_xid ON branch_table (xid);

-- 全局锁表
CREATE TABLE IF NOT EXISTS lock_table
(
    row_key        VARCHAR(128) NOT NULL,
    xid            VARCHAR(128),
    transaction_id BIGINT,
    branch_id      BIGINT       NOT NULL,
    resource_id    VARCHAR(256),
    table_name     VARCHAR(32),
    pk             VARCHAR(36),
    status         SMALLINT     NOT NULL DEFAULT 0,
    gmt_create     TIMESTAMP,
    gmt_modified   TIMESTAMP,
    PRIMARY KEY (row_key)
    );
CREATE INDEX IF NOT EXISTS idx_lock_status ON lock_table (status);
CREATE INDEX IF NOT EXISTS idx_lock_branch_id ON lock_table (branch_id);
CREATE INDEX IF NOT EXISTS idx_lock_xid ON lock_table (xid);

-- 分布式锁（Seata HA 用）
CREATE TABLE IF NOT EXISTS distributed_lock
(
    lock_key   VARCHAR(20)  NOT NULL,
    lock_value VARCHAR(20)  NOT NULL,
    expire     BIGINT,
    PRIMARY KEY (lock_key)
    );

INSERT INTO distributed_lock (lock_key, lock_value, expire)
VALUES ('AsyncCommitting', ' ', 0),
       ('RetryCommitting', ' ', 0),
       ('RetryRollbacking', ' ', 0),
       ('TxTimeoutCheck', ' ', 0)
    ON CONFLICT (lock_key) DO NOTHING;

-- Seata AT事务模式回滚日志表
CREATE TABLE IF NOT EXISTS public.undo_log
(
    id            SERIAL       NOT NULL,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info BYTEA        NOT NULL,
    log_status    INT          NOT NULL,
    log_created   TIMESTAMP(0) NOT NULL,
    log_modified  TIMESTAMP(0) NOT NULL,
    CONSTRAINT pk_undo_log PRIMARY KEY (id),
    CONSTRAINT ux_undo_log UNIQUE (xid, branch_id)
    );

CREATE INDEX IF NOT EXISTS ix_log_created ON public.undo_log (log_created);

COMMENT ON TABLE public.undo_log IS 'Seata AT事务模式回滚日志表';
COMMENT ON COLUMN public.undo_log.branch_id IS '分支事务ID';
COMMENT ON COLUMN public.undo_log.xid IS '全局事务ID';
COMMENT ON COLUMN public.undo_log.context IS '回滚日志上下文';
COMMENT ON COLUMN public.undo_log.rollback_info IS '回滚信息';
COMMENT ON COLUMN public.undo_log.log_status IS '日志状态：0正常，1防悬挂';
COMMENT ON COLUMN public.undo_log.log_created IS '创建时间';
COMMENT ON COLUMN public.undo_log.log_modified IS '更新时间';

CREATE SEQUENCE IF NOT EXISTS public.undo_log_id_seq INCREMENT BY 1 MINVALUE 1;