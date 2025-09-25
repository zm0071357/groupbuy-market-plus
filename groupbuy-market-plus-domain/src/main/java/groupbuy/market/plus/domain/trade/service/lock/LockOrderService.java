package groupbuy.market.plus.domain.trade.service.lock;

import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderDetailEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;

public interface LockOrderService {

    /**
     * 锁单
     * @param userEntity 用户信息
     * @param groupBuyTeamEntity 拼团组队信息
     * @param orderDetailEntity 订单详情信息
     * @return
     */
    LockOrderEntity lockOrder(UserEntity userEntity, GroupBuyTeamEntity groupBuyTeamEntity, OrderDetailEntity orderDetailEntity) throws Exception;

    /**
     * 获取未支付的锁单订单
     * @param userId 用户ID
     * @param outTradeNo 外部交易单号
     * @return
     */
    LockOrderEntity getNoPayLockOrderByOutTradeNo(String userId, String outTradeNo);

    /**
     * 获取拼团进度
     * @param teamId 组队ID
     * @return
     */
    TeamProgressVO getTeamProgress(String teamId);
}
