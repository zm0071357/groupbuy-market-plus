package groupbuy.market.plus.domain.trade.model.valobj;

/** 免拼机会与累计进度，按用户、来源、渠道计算。 */
public record SkipGroupBalanceVO(long available, long ordersUntilNext) {

    /** 每两笔有效订单换一次机会，已占用和已消耗机会计入实际欠账。 */
    public static SkipGroupBalanceVO from(long earned, long held, long used) {
        long available = Math.max(0, earned / 2 - held - used);
        long next = 2 * (available + held + used + 1) - earned;
        return new SkipGroupBalanceVO(available, next);
    }
}
