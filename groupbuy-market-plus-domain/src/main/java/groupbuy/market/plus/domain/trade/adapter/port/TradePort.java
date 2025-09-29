package groupbuy.market.plus.domain.trade.adapter.port;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;

public interface TradePort {

    /**
     * 执行回调任务
     * @param notifyTaskEntity
     * @return
     * @throws Exception
     */
    String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception;

}
