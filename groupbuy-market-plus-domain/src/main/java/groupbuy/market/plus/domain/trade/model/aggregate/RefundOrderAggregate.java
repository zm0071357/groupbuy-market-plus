package groupbuy.market.plus.domain.trade.model.aggregate;

import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderAggregate {

    /**
     * 退单订单
     */
    private RefundOrderEntity refundOrderEntity;

    /**
     * 拼团组队状态枚举
     */
    private TeamStatusEnum teamStatusEnum;

    /**
     * 拼团进度
     */
    private TeamProgressVO teamProgressVO;

    /**
     * 退单聚合 - 拼团组队未完成 - 未支付
     * @param refundOrderEntity
     * @param lockCount
     * @return
     */
    public static RefundOrderAggregate buildTeamInCompleteUnPaidRefundAggregate(RefundOrderEntity refundOrderEntity, Integer lockCount) {
        return RefundOrderAggregate.builder()
                .refundOrderEntity(refundOrderEntity)
                .teamProgressVO(TeamProgressVO.builder()
                        .lockCount(lockCount)
                        .build())
                .build();
    }

    /**
     * 退单聚合 - 拼团组队未完成 - 已支付
     * @param refundOrderEntity
     * @param lockCount
     * @param completeCount
     * @return
     */
    public static RefundOrderAggregate buildTeamInCompletePaidRefundAggregate(RefundOrderEntity refundOrderEntity, Integer lockCount, Integer completeCount) {
        return RefundOrderAggregate.builder()
                .refundOrderEntity(refundOrderEntity)
                .teamProgressVO(TeamProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build())
                .build();
    }

    /**
     * 退单聚合 - 拼团组队完成 - 已支付
     * @param refundOrderEntity
     * @param lockCount
     * @param completeCount
     * @param teamStatusEnum
     * @return
     */
    public static RefundOrderAggregate buildTeamCompletePaidRefundAggregate(RefundOrderEntity refundOrderEntity, Integer lockCount, Integer completeCount, TeamStatusEnum teamStatusEnum) {
        return RefundOrderAggregate.builder()
                .refundOrderEntity(refundOrderEntity)
                .teamProgressVO(TeamProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build())
                .teamStatusEnum(teamStatusEnum)
                .build();
    }
}
