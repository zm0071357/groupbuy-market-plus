package groupbuy.market.plus.domain.trade.adapter.port;

import java.util.OptionalLong;

/** 从商城查询买家在指定店铺的历史有效下单次数。 */
public interface ShopOrderCountPort {

    /** 当前正在锁定的商城订单号不计入结果，无法确认时返回空。 */
    OptionalLong count(String shopId, String userId, String excludedOutTradeNo);
}
