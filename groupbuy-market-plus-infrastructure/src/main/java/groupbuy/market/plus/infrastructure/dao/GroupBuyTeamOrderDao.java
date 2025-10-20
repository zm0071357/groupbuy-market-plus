package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 根据组队ID获取拼团完成的外部交易单号列表
     * @param teamId 组队ID
     * @return
     */
    List<String> getCompleteTeamOutTradeNoList(String teamId);

    /**
     * 查询用户的组队ID集合
     * @param groupBuyTeamOrderReq
     * @return
     */
    List<String> getUserTeamIdList(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 查询用户未参与的组队ID集合
     * @param groupBuyTeamOrderReq
     * @param limitCount
     * @return
     */
    List<String> getRandomTeamIdList(@Param("groupBuyTeamOrderReq") GroupBuyTeamOrder groupBuyTeamOrderReq, @Param("limitCount") Integer limitCount);

    /**
     * 查询预退单订单
     * @param groupBuyTeamOrderReq
     * @return
     */
    GroupBuyTeamOrder getPreRefundOrder(GroupBuyTeamOrder groupBuyTeamOrderReq);

    /**
     * 获取新团长ID
     * @param teamId 拼团组队ID
     * @return
     */
    String getNewHeaderUserId(@Param("teamId") String teamId);

    /**
     * 更新用户为新团长
     * @param newHeaderUserId 新团长用户ID
     */
    Integer updateUserIsHeader(@Param("userId") String newHeaderUserId);

    /**
     * 更新订单状态为退单
     * @param groupBuyTeamOrderReq
     * @return
     */
    Integer updateOrderStatusRefund(GroupBuyTeamOrder groupBuyTeamOrderReq);
}
