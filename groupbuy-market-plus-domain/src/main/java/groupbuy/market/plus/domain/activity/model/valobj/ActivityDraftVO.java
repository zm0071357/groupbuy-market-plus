package groupbuy.market.plus.domain.activity.model.valobj;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 运营档位活动的完整草稿，时间使用 UTC 时间点。 */
public record ActivityDraftVO(String name, String goodsId, String source, String channel,
                              Instant startTime, Instant endTime, Integer validMinutes, Integer userLimit,
                              List<GroupBuyTierVO> tiers, Boolean skipGroupEnabled, BigDecimal skipGroupPrice, String eligibilityShopId) {
}
