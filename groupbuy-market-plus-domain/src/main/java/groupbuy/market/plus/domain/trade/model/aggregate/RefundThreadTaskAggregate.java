package groupbuy.market.plus.domain.trade.model.aggregate;

import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单领域 - 异步任务结果聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundThreadTaskAggregate {

    /**
     * 预退单订单
     */
    private PreRefundOrderEntity preRefundOrderEntity;

    /**
     * 拼团组队
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;

}
