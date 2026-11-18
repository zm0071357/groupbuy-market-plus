package groupbuy.market.plus.domain.activity.model.valobj;

import groupbuy.market.plus.types.common.GroupBuyConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 拼团价格规则，供商品页展示与锁单金额计算复用。 */
public final class GroupBuyPrice {

    private GroupBuyPrice() {
    }

    /** 判断活动报价能否按分计价，且在最低支付价和商品原价之间。 */
    public static boolean isValidOffer(BigDecimal price, BigDecimal originalPrice) {
        return price != null && originalPrice != null && price.stripTrailingZeros().scale() <= 2
                && price.compareTo(GroupBuyConstants.MinPrice) >= 0 && price.compareTo(originalPrice) <= 0;
    }

    /** 团长在团员价基础上享额外折扣，实付金额保留两位小数且最低为 0.01 元。 */
    public static BigDecimal leaderPrice(BigDecimal memberPrice) {
        return memberPrice.multiply(GroupBuyConstants.HeaderDiscount)
                .setScale(2, RoundingMode.HALF_UP)
                .max(GroupBuyConstants.MinPrice);
    }
}
