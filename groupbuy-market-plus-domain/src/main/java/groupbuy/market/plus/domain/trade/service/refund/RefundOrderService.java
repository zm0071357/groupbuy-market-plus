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
     * 将超时且订单已全部退单的拼团组队标记为失败
     * 需在超时退单、邀请码失效之后执行：未成团退单要求组队为拼单中，邀请码失效按拼单中的超时组队查询
     * @return 标记为失败的组队数量
     */
    Integer teamTimeoutFail();

    /**
     * 获取拼团组队进度
     * @param teamId 拼团组队ID
     * @return
     */
    TeamProgressVO getTeamProgress(String teamId);
}
