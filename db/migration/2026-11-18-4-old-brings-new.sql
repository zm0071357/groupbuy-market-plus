-- 先执行档位、运营管理及免拼迁移。历史活动为空，继续按不限新老客的规则交易。
ALTER TABLE sc_sku_activity
    ADD COLUMN eligibility_shop_id VARCHAR(64) NULL COMMENT '老带新活动所属店铺，NULL 为不限新老客';

-- 开团后固定资格所属店铺；历史组队及普通组队默认不限。
ALTER TABLE group_buy_team
    ADD COLUMN eligibility_shop_id VARCHAR(64) NULL COMMENT '开团时保存的老带新店铺';
