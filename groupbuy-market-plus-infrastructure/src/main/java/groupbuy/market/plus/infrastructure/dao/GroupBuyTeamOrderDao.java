package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupBuyTeamOrderDao {

    /**
     * 用户参与活动次数
     * @param groupBuyTeamOrderReq
     * @return
     */
    Integer checkUserTakeActivityCount(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 用户参与自身拼团次数
     * @param groupBuyTeamOrderReq
     * @return
     */
    Integer checkUserTakeTeamCount(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 新增用户拼团订单
     * @param groupBuyTeamOrder
     */
    void insert(GroupBuyTeamOrder groupBuyTeamOrder);

    /**
     * 获取未支付的锁单订单
     * @param groupBuyTeamOrderReq
     * @return
     */
    GroupBuyTeamOrder getNoPayLockOrderByOutTradeNo(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 锁单订单的支付情况
     * @param groupBuyTeamOrderReq
     * @return
     */
    GroupBuyTeamOrder checkLockOrderStatusByOutTradeNo(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 更新订单状态为消费完成
     * @param groupBuyTeamOrderReq
     * @return
     */
    Integer updateOrderStatusComplete(GroupBuyTeamOrder groupBuyTeamOrderReq);
}
