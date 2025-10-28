package groupbuy.market.plus.domain.trade.adapter.repository;

import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundThreadTaskAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;

import java.util.Date;
import java.util.List;

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
    NotifyTaskEntity settleOrder(SettleOrderAggregate settleOrderAggregate);

    /**
     * 获取未完成的拼团完成回调通知任务集合
     * @return
     */
    List<NotifyTaskEntity> getUnNotifyTeamSuccessTask();

    /**
     * 更新回调任务状态为完成
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskSuccess(String teamId);

    /**
     * 更新回调任务状态为重试
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskRetry(String teamId);

    /**
     * 更新回调任务状态为失败
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskFail(String teamId);

    /**
     * 抢占库存
     * @param target 目标量
     * @param validTime 拼团有效时间
     * @param teamStockOccupyKey 抢占Key
     * @param teamStockRecoverKey 恢复Key
     * @return
     */
    boolean occupyTeamStock(Integer target, Integer validTime, String teamStockOccupyKey, String teamStockRecoverKey);

    /**
     * 锁单失败时恢复可用位置
     * @param teamStockRecoverKey 恢复Key
     */
    Long recoverTeamStock(String teamStockRecoverKey);

    /**
     * 获取退单所需数据聚合
     * @param preRefundEntity
     * @return
     */
    RefundThreadTaskAggregate getRefundThreadTaskResAggregate(PreRefundEntity preRefundEntity);

    /**
     * 选出新团长
     * @param teamId 拼团组队ID
     * @return
     */
    String getNewHeaderUser(String teamId);

    /**
     * 退单 - 拼团组队未完成 - 未支付
     * @param teamInCompleteUnPaidRefundAggregate 退单聚合
     * @return
     */
    NotifyTaskEntity teamInCompleteUnPaidRefund(RefundOrderAggregate teamInCompleteUnPaidRefundAggregate);

    /**
     * 退单 - 拼团组队未完成 - 已支付
     * @param teamInCompletePaidRefundAggregate 退单聚合
     * @return
     */
    NotifyTaskEntity teamInCompletePaidRefund(RefundOrderAggregate teamInCompletePaidRefundAggregate);

    /**
     * 退单 - 拼团组队完成 - 已支付
     * @param teamCompletePaidRefundAggregate 退单聚合
     * @return
     */
    NotifyTaskEntity teamCompletePaidRefund(RefundOrderAggregate teamCompletePaidRefundAggregate);

    /**
     * 新团长回调
     * @param newLeaderUserId 新团长ID
     * @param orderId 订单ID
     * @param groupBuyTeamEntity 拼团组队信息
     * @return
     */
    NotifyTaskEntity newHeaderNotify(String newLeaderUserId, String orderId, GroupBuyTeamEntity groupBuyTeamEntity);

    /**
     * 获取超时的拼团ID集合
     * @return
     */
    List<String> getTimeoutTeamIdList();

    /**
     * 获取超时的拼团订单集合
     * @param timeoutTeamIdList 超时的拼团ID集合
     * @return
     */
    List<PreRefundEntity> getTimeoutOrderList(List<String> timeoutTeamIdList);

    /**
     * 获取未完成的退单回调通知任务集合
     * @return
     */
    List<NotifyTaskEntity> getUnNotifyOrderRefundTask();

    /**
     * 获取未完成的团长退单补偿回调通知任务集合
     * @return
     */
    List<NotifyTaskEntity> getUnNotifyHeaderRefundTask();

    /**
     * 更新订单的退款外部单号和退款完成时间
     * @param userId 用户ID
     * @param outTradeNo 外部交易单号
     * @param outRefundNo 退款外部交易单号
     * @param outRefundNoCompleteTime 退款完成时间
     */
    void updateRefundNoAndRefundTime(String userId, String outTradeNo, String outRefundNo, Date outRefundNoCompleteTime);

    /**
     * 生成唯一邀请码
     * @param userId 邀请人ID
     * @param teamId 拼团组队ID
     * @return
     */
    InviteEntity invite(String userId, String teamId);

    /**
     * 检查邀请人是否在拼团组队中
     * @param inviteUserId 邀请人ID
     * @param teamId 拼团组队ID
     * @return
     */
    Integer checkUserInTeam(String inviteUserId, String teamId);

    /**
     * 获取邀请返利
     * @param inviteId 邀请码信息
     * @return
     */
    InviteEntity getInvite(String inviteId);

    /**
     * 邀请返利失效
     * @param timeoutTeamIdList 超时的拼团ID集合
     */
    void inviteExpire(List<String> timeoutTeamIdList);
}
