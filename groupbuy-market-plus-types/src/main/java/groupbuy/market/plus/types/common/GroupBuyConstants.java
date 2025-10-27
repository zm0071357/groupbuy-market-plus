package groupbuy.market.plus.types.common;

import java.math.BigDecimal;

/**
 * 拼团常量
 */
public class GroupBuyConstants {

    public static final String TEAM = "TEAM";

    public static final String ORDER = "PHW";

    public static final BigDecimal HeaderDiscount = BigDecimal.valueOf(0.95);

    public static final String HEADER = "HEADER";

    public static final String MEMBER = "MEMBER";

    public static final BigDecimal MinPrice = BigDecimal.valueOf(0.01);

    public static final String TeamSuccessNotifyJobLock = "groupbuy_market_plus_team_success_notify_job_lock";

    public static final String TeamTimeoutRefundJobLock = "groupbuy_market_plus_team_timeout_refund_job_lock";

    public static final String HeaderRefundNotifyJobLock = "groupbuy_market_plus_header_refund_notify_job_lock";

    public static final String OrderRefundNotifyJobLock = "groupbuy_market_plus_order_refund_notify_job_lock";

    public static final String TeamStockKey = "groupbuy_market_plus_stock_key_";

}
