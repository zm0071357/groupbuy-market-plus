package groupbuy.market.plus.domain.trade.service.settle;

import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.model.entity.SettleOrderEntity;

import java.util.Map;

public interface SettleOrderService {

    /**
     * 结算
     * @param orderPaySuccessEntity
     * @return
     */
    SettleOrderEntity settleOrder(OrderPaySuccessEntity orderPaySuccessEntity) throws Exception;

    /**
     * 执行定时任务 - 回调通知拼团完成
     * @return
     */
    Map<String, Integer> execNotifyJob() throws Exception;

    /**
     * 指定组队ID执行回调通知拼团完成
     * @param teamId 组队ID
     * @return
     */
    Map<String, Integer> execNotifyJob(String teamId) throws Exception;
}
