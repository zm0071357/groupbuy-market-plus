-- 先执行档位与运营管理迁移。已有活动未配置免拼价，历史组队均为普通或档位团。
ALTER TABLE sc_sku_activity
    ADD COLUMN skip_group_price DECIMAL(12,2) NULL COMMENT '免拼实付价，NULL 表示禁用';

ALTER TABLE group_buy_team
    ADD COLUMN skip_group TINYINT NOT NULL DEFAULT 0 COMMENT '是否为免拼组队';

-- 机会按用户、来源和渠道累计，计数由订单事务原子更新。
CREATE TABLE group_buy_skip_account (
    user_id VARCHAR(64) NOT NULL,
    source VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    earned_count BIGINT NOT NULL DEFAULT 0,
    held_count BIGINT NOT NULL DEFAULT 0,
    used_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, source, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 旧订单不导入。订单号键约束防止同一订单重复累计或占用。
CREATE TABLE group_buy_skip_record (
    order_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    source VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    state VARCHAR(12) NOT NULL COMMENT 'EARNED、REVOKED、HELD、USED、RELEASED',
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
