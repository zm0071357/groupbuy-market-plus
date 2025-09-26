package groupbuy.market.plus.domain.trade.model.aggregate;

import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结算聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettleOrderAggregate {

    /**
     * 用户信息
     */
    private UserEntity userEntity;

    /**
     * 拼团组队信息
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;

    /**
     * 完成支付订单信息
     */
    private OrderPaySuccessEntity orderPaySuccessEntity;
}
