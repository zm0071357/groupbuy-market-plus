-- 先加列再部署应用。历史活动映射和组队均保持 NULL，可继续普通拼团。
ALTER TABLE sc_sku_activity
    ADD COLUMN tiers_json JSON NULL COMMENT '自选拼团档位，按商品、来源和渠道配置';

-- 组队沿用 target_count、original_price 和 pay_price 保存开团时的人数、原价和团员价。
ALTER TABLE group_buy_team
    ADD COLUMN tier_id BIGINT NULL COMMENT '开团时选择的档位 ID，普通团为 NULL';
