package groupbuy.market.plus.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 独立运营后台使用的档位活动协议。 */
public final class ActivityAdminDTO {

    private ActivityAdminDTO() {}

    /** 活动配置，时间带时区，validMinutes 单位为分钟。 */
    public record Draft(String name, String goodsId, String source, String channel, Instant startTime, Instant endTime,
                        Integer validMinutes, Integer userLimit, List<Tier> tiers) {}

    /** 档位价格单位为元，leaderPrice 由服务端计算。 */
    public record Tier(Long tierId, Integer targetCount, BigDecimal memberPrice, BigDecimal leaderPrice) {}

    /** 可选商品。 */
    public record Product(Long id, String goodsId, String name, String source, String channel, BigDecimal originalPrice) {}

    /** 管理记录，未发布时 activityId 为空。 */
    public record Activity(Long id, Long activityId, String state, Draft draft) {}

    /** 创建草稿或发布后返回的编号。 */
    public record Id(Long id) {}
}
