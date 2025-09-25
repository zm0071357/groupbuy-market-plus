package groupbuy.market.plus.domain.trade.model.aggregate;

import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderDetailEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁单聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockOrderAggregate {

    /**
     * 用户信息
     */
    private UserEntity userEntity;

    /**
     * 拼团组队信息
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;

    /**
     * 锁单订单详情信息
     */
    private OrderDetailEntity orderDetailEntity;

    /**
     * 锁单返回结果
     */
    private CheckLockResEntity checkLockResEntity;

}
