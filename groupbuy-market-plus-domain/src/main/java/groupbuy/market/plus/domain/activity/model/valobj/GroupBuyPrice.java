package groupbuy.market.plus.domain.activity.model.valobj;

import groupbuy.market.plus.types.common.GroupBuyConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 拼团价格规则，供商品页展示与锁单金额计算复用。 */
public final class GroupBuyPrice {

    private GroupBuyPrice() {
    }

    /** 团长在团员价基础上享额外折扣，实付金额保留两位小数且最低为 0.01 元。 */
    public static BigDecimal leaderPrice(BigDecimal memberPrice) {
        return memberPrice.multiply(GroupBuyConstants.HeaderDiscount)
                .setScale(2, RoundingMode.HALF_UP)
                .max(GroupBuyConstants.MinPrice);
    }
}
