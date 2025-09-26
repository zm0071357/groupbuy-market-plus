package groupbuy.market.plus.domain.trade.adapter.repository;

import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.SettleOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;

public interface TradeRepository {

    /**
     * 根据活动ID获取活动
     * @param activityId 活动ID
     * @return
     */
    ActivityEntity getActivityById(Long activityId);

    /**
     * 用户参与活动次数
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return
     */
    Integer checkUserTakeActivityCount(Long activityId, String userId);

    /**
     * 用户参与自身拼团次数
     * @param teamId 组队ID
     * @param userId 用户ID
     * @return
     */
    Integer checkUserTakeTeamCount(String teamId, String userId);

    /**
     * 锁单
     * @param lockOrderAggregate
     * @return
     */
    LockOrderEntity lockOrder(LockOrderAggregate lockOrderAggregate);

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

    /**
     * 锁单订单的支付情况
     * @param userId 用户ID
     * @param outTradeNo 外部交易单号
     * @return
     */
    LockOrderEntity checkLockOrderStatusByOutTradeNo(String userId, String outTradeNo);

    /**
     * 根据组队ID获取组队
     * @param teamId 组队ID
     * @return
     */
    GroupBuyTeamEntity getTeamById(String teamId);

    /**
     * SC黑名单
     * @param resource 来源
     * @param channel 渠道
     * @return
     */
    boolean isBlack(String resource, String channel);

    /**
     * 结算
     * @param settleOrderAggregate
     * @return
     */
    SettleOrderEntity settleOrder(SettleOrderAggregate settleOrderAggregate);
}
