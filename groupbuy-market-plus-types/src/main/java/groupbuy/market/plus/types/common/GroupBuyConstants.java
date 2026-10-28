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

    // 锁单用户维度锁前缀 - 拼接 活动ID_用户ID
    public static final String LockOrderUserLockPrefix = "groupbuy_market_plus_lock_order_user_lock_";

    /**
     * 生成邀请码锁前缀 - 按「组队 + 用户」加锁，防止并发生成多条有效邀请码
     */
    public static final String InviteUserLockPrefix = "groupbuy_market_plus_invite_user_lock_";

    /**
     * 超时组队分页查询每页数量 - 超时退单、邀请码失效按组队ID游标分批处理，避免积压时生成超长 IN 语句
     */
    public static final int TimeoutTeamPageSize = 200;

    public static final String TeamStockKey = "groupbuy_market_plus_stock_key_";

}
