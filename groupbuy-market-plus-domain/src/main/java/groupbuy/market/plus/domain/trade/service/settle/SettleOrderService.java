package groupbuy.market.plus.domain.trade.service.settle;

import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.model.entity.SettleOrderEntity;

public interface SettleOrderService {

    /**
     * 结算
     * @param orderPaySuccessEntity
     * @return
     */
    SettleOrderEntity settleOrder(OrderPaySuccessEntity orderPaySuccessEntity) throws Exception;

}
