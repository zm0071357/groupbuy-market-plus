package groupbuy.market.plus.domain.trade.service.refund;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;

import java.util.Map;

public interface RefundOrderService {

    /**
     * 退单
     * @param preRefundEntity
     * @return
     */
    RefundResEntity refundOrder(PreRefundEntity preRefundEntity) throws Exception;

    /**
     * 恢复锁单量
     * @param orderRefundMessage
     */
    void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception;

    /**
     * 拼团超时失败进行退单
     */
    Map<String, Integer> teamTimeoutRefund() throws Exception;

    /**
     * 获取拼团组队进度
     * @param teamId 拼团组队ID
     * @return
     */
    TeamProgressVO getTeamProgress(String teamId);
}
