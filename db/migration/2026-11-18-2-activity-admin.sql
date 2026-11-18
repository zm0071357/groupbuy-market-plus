-- 先执行 2026-11-18-tiered-group-buy.sql。添加唯一索引前按 README 核对历史数据。
ALTER TABLE activity
    ADD COLUMN opening_enabled TINYINT NULL COMMENT '运营活动是否允许开新团，旧活动为 NULL',
    ADD UNIQUE KEY uk_ops_activity_id (activity_id);

ALTER TABLE sc_sku_activity
    ADD UNIQUE KEY uk_ops_goods_route (goods_id, source, channel);

CREATE TABLE group_buy_activity_admin (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '草稿编号，发布后同时用作活动编号',
    activity_id BIGINT NULL COMMENT '发布后的业务活动编号',
    state VARCHAR(12) NOT NULL COMMENT 'DRAFT、PUBLISHED、OFFLINE',
    draft_json JSON NOT NULL COMMENT '活动草稿及档位配置',
    PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1000000000;
