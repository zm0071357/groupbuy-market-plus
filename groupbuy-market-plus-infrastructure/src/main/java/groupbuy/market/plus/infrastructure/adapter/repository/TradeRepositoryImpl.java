package groupbuy.market.plus.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.ActivityStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.infrastructure.dao.ActivityDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.Activity;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import groupbuy.market.plus.infrastructure.dcc.DCCServiceImpl;
import groupbuy.market.plus.types.common.Constants;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class TradeRepositoryImpl implements TradeRepository {

    @Resource
    private ActivityDao activityDao;

    @Resource
    private GroupBuyTeamDao groupBuyTeamDao;

    @Resource
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;

    @Resource
    private NotifyTaskDao notifyTaskDao;

    @Resource
    private DCCServiceImpl dccServiceImpl;

    @Override
    public ActivityEntity getActivityById(Long activityId) {
        Activity activity = activityDao.getActivityById(activityId);
        if (activity == null) {
            return null;
        }
        return ActivityEntity.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .discountExpr(activity.getDiscountExpr())
                .groupType(activity.getGroupType())
                .takeLimitCount(activity.getTakeLimitCount())
                .target(activity.getTarget())
                .validTime(activity.getValidTime())
                .activityStatusEnum(ActivityStatusEnum.valueOf(activity.getStatus()))
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .tagId(activity.getTagId())
                .tagScope(activity.getTagScope())
                .build();
    }

    @Override
    public Integer checkUserTakeActivityCount(Long activityId, String userId) {
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setActivityId(activityId);
        groupBuyTeamOrderReq.setUserId(userId);
        return groupBuyTeamOrderDao.checkUserTakeActivityCount(groupBuyTeamOrderReq);
    }

    @Override
    public Integer checkUserTakeTeamCount(String teamId, String userId) {
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setTeamId(teamId);
        groupBuyTeamOrderReq.setUserId(userId);
        return groupBuyTeamOrderDao.checkUserTakeTeamCount(groupBuyTeamOrderReq);
    }

    @Transactional(timeout = 500)
    @Override
    public LockOrderEntity lockOrder(LockOrderAggregate lockOrderAggregate) {
        UserEntity userEntity = lockOrderAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = lockOrderAggregate.getGroupBuyTeamEntity();
        OrderDetailEntity orderDetailEntity = lockOrderAggregate.getOrderDetailEntity();
        CheckLockResEntity checkLockResEntity = lockOrderAggregate.getCheckLockResEntity();
        log.info("锁单开始，用户ID：{}", userEntity.getUserId());

        // 计算拼团组队的有效期
        Date currentTime = new Date();
        Calendar calender = Calendar.getInstance();
        calender.setTime(currentTime);
        calender.add(Calendar.MINUTE, groupBuyTeamEntity.getValidTime());
        String teamId = groupBuyTeamEntity.getTeamId();

        // 团长 - 新建一个团，锁单量为1
        if (checkLockResEntity.getIsHeader() && StringUtils.isBlank(teamId)) {
            log.info("用户ID：{}为团长，新建一个团", userEntity.getUserId());
            teamId = GroupBuyConstants.TEAM.concat(RandomStringUtils.randomNumeric(8));
            // 拼团组队写入数据库
            groupBuyTeamDao.insert(GroupBuyTeam.builder()
                    .teamId(teamId)
                    .activityId(groupBuyTeamEntity.getActivityId())
                    .goodsId(groupBuyTeamEntity.getGoodsId())
                    .source(orderDetailEntity.getSource())
                    .channel(orderDetailEntity.getChannel())
                    .originalPrice(orderDetailEntity.getOriginalPrice())
                    .deductionPrice(orderDetailEntity.getDeductionPrice())
                    .payPrice(orderDetailEntity.getPayPrice())
                    .targetCount(groupBuyTeamEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .startTime(currentTime)
                    .endTime(calender.getTime())
                    .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
                    .build());
        } else {
            // 团员 - 更新锁单量
            log.info("用户ID：{}为团员，更新锁单数", userEntity.getUserId());
            Integer updateCount = groupBuyTeamDao.updateLockCount(groupBuyTeamEntity.getTeamId());
            if (updateCount != 1) {
                throw new AppException(ResponseCodeEnum.E0011.getCode(), ResponseCodeEnum.E0011.getInfo());
            }
        }

        // 用户拼团订单写入数据库
        String orderId = GroupBuyConstants.ORDER.concat(RandomStringUtils.randomNumeric(12));
        // 团长有额外优惠
        BigDecimal payPrice = calculatePayPrice(checkLockResEntity.getIsHeader(), orderDetailEntity.getPayPrice());
        String sign = checkLockResEntity.getIsHeader() ? GroupBuyConstants.HEADER : GroupBuyConstants.MEMBER;
        String bizId = groupBuyTeamEntity.getActivityId() + Constants.UNDERSCORE +
                userEntity.getUserId() + Constants.UNDERSCORE +
                teamId + Constants.UNDERSCORE +
                sign;
        try {
            groupBuyTeamOrderDao.insert(GroupBuyTeamOrder.builder()
                    .userId(userEntity.getUserId())
                    .teamId(teamId)
                    .orderId(orderId)
                    .activityId(groupBuyTeamEntity.getActivityId())
                    .startTime(groupBuyTeamEntity.getStartTime())
                    .endTime(groupBuyTeamEntity.getEndTime())
                    .goodsId(orderDetailEntity.getGoodsId())
                    .source(orderDetailEntity.getSource())
                    .channel(orderDetailEntity.getChannel())
                    .originalPrice(orderDetailEntity.getOriginalPrice())
                    .deductionPrice(orderDetailEntity.getDeductionPrice())
                    .payPrice(payPrice)
                    .isHeader(checkLockResEntity.getIsHeader() ? 1 : 0)
                    .status(OrderStatusEnum.CREATE.getCode())
                    .outTradeNo(orderDetailEntity.getOutTradeNo())
                    .bizId(bizId)
                    .build());
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCodeEnum.INDEX_EXCEPTION.getCode(), ResponseCodeEnum.INDEX_EXCEPTION.getInfo());
        }

        return LockOrderEntity.builder()
                .teamId(teamId)
                .orderId(orderId)
                .isHeader(checkLockResEntity.getIsHeader())
                .payPrice(payPrice)
                .orderStatusEnum(OrderStatusEnum.CREATE)
                .build();
    }

    /**
     * 根据是否为团长进行最终支付价格计算
     * @param isHeader 是否为团长
     * @param payPrice 支付价格
     * @return
     */
    private BigDecimal calculatePayPrice(Boolean isHeader, BigDecimal payPrice) {
        // 团长有额外优惠
        if (isHeader) {
            BigDecimal endPrice = payPrice.multiply(GroupBuyConstants.HeaderDiscount);
            if (endPrice.compareTo(BigDecimal.ZERO) < 0) {
                return GroupBuyConstants.MinPrice;
            }
            return endPrice;
        }
        return payPrice;
    }

    @Override
    public LockOrderEntity getNoPayLockOrderByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(userId);
        groupBuyTeamOrderReq.setOutTradeNo(outTradeNo);
        GroupBuyTeamOrder groupBuyTeamOrder = groupBuyTeamOrderDao.getNoPayLockOrderByOutTradeNo(groupBuyTeamOrderReq);
        if (groupBuyTeamOrder == null) {
            return null;
        }
        return LockOrderEntity.builder()
                .teamId(groupBuyTeamOrder.getTeamId())
                .orderId(groupBuyTeamOrder.getOrderId())
                .isHeader(groupBuyTeamOrder.getIsHeader() == 1)
                .payPrice(groupBuyTeamOrder.getPayPrice())
                .orderStatusEnum(OrderStatusEnum.valueOf(groupBuyTeamOrder.getStatus()))
                .build();
    }

    @Override
    public TeamProgressVO getTeamProgress(String teamId) {
        GroupBuyTeam groupBuyTeam = groupBuyTeamDao.getTeamProgress(teamId);
        if (groupBuyTeam == null) {
            return null;
        }
        return TeamProgressVO.builder()
                .targetCount(groupBuyTeam.getTargetCount())
                .completeCount(groupBuyTeam.getCompleteCount())
                .lockCount(groupBuyTeam.getLockCount())
                .build();
    }

    @Override
    public LockOrderEntity checkLockOrderStatusByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(userId);
        groupBuyTeamOrderReq.setOutTradeNo(outTradeNo);
        GroupBuyTeamOrder groupBuyTeamOrder = groupBuyTeamOrderDao.checkLockOrderStatusByOutTradeNo(groupBuyTeamOrderReq);
        if (groupBuyTeamOrder == null) {
            return null;
        }
        return LockOrderEntity.builder()
                .teamId(groupBuyTeamOrder.getTeamId())
                .orderId(groupBuyTeamOrder.getOrderId())
                .isHeader(groupBuyTeamOrder.getIsHeader() == 1)
                .payPrice(groupBuyTeamOrder.getPayPrice())
                .outTradeNo(groupBuyTeamOrder.getOutTradeNo())
                .outTradeNoPayTime(groupBuyTeamOrder.getOutTradeNoPayTime())
                .orderStatusEnum(OrderStatusEnum.valueOf(groupBuyTeamOrder.getStatus()))
                .build();
    }

    @Override
    public GroupBuyTeamEntity getTeamById(String teamId) {
        GroupBuyTeam groupBuyTeam = groupBuyTeamDao.getTeamById(teamId);
        if (groupBuyTeam == null) {
            return null;
        }
        return GroupBuyTeamEntity.builder()
                .teamId(teamId)
                .activityId(groupBuyTeam.getActivityId())
                .startTime(groupBuyTeam.getStartTime())
                .endTime(groupBuyTeam.getEndTime())
                .targetCount(groupBuyTeam.getTargetCount())
                .completeCount(groupBuyTeam.getCompleteCount())
                .lockCount(groupBuyTeam.getLockCount())
                .status(groupBuyTeam.getStatus())
                .build();
    }

    @Override
    public boolean isBlack(String resource, String channel) {
        return dccServiceImpl.isBlack(resource, channel);
    }

    @Transactional(timeout = 500)
    @Override
    public SettleOrderEntity settleOrder(SettleOrderAggregate settleOrderAggregate) {
        UserEntity userEntity = settleOrderAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = settleOrderAggregate.getGroupBuyTeamEntity();
        OrderPaySuccessEntity orderPaySuccessEntity = settleOrderAggregate.getOrderPaySuccessEntity();
        log.info("结算开始，用户ID：{}，外部交易单号：{}", userEntity.getUserId(), orderPaySuccessEntity.getOutTradeNo());

        // 更新订单状态为消费完成
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(orderPaySuccessEntity.getUserId());
        groupBuyTeamOrderReq.setOutTradeNo(orderPaySuccessEntity.getOutTradeNo());
        groupBuyTeamOrderReq.setOutTradeNoPayTime(orderPaySuccessEntity.getOutTradeNoPayTime());
        Integer updateOrderStatusCount = groupBuyTeamOrderDao.updateOrderStatusComplete(groupBuyTeamOrderReq);
        if (updateOrderStatusCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 更新拼团组队进度
        Integer updateTeamProgressCount = groupBuyTeamDao.updateTeamAddCompleteCount(groupBuyTeamEntity.getTeamId());
        if (updateTeamProgressCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 最后一笔 - 拼团成功
        boolean isComplete = false;     // 拼团是否完成
        if (groupBuyTeamEntity.getTargetCount() - groupBuyTeamEntity.getCompleteCount() == 1) {
            log.info("拼团目标完成，组队ID：{}", groupBuyTeamEntity.getTeamId());
            // 更新拼团组队为完成
            Integer updateTeamStatusCount = groupBuyTeamDao.updateTeamStatusComplete(groupBuyTeamEntity.getTeamId());
            if (updateTeamStatusCount != 1) {
                throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
            }
            // 回调任务
            List<String> outTradeNoList = groupBuyTeamOrderDao.getCompleteTeamOutTradeNoList(groupBuyTeamEntity.getTeamId());
            NotifyTask notifyTask = NotifyTask.builder()
                    .activityId(groupBuyTeamEntity.getActivityId())
                    .teamId(groupBuyTeamEntity.getTeamId())
                    .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
                    .notifyCount(0)
                    .notifyStatus(0)
                    .parameterJson(JSON.toJSONString(new HashMap<String, Object>(){{
                        put("teamId", groupBuyTeamEntity.getTeamId());
                        put("outTradeNoList", outTradeNoList);
                    }}))
                    .build();
            notifyTaskDao.insert(notifyTask);
            isComplete = true;
            log.info("初始化拼团回调任务，拼团组队ID：{}", groupBuyTeamEntity.getTeamId());
        }

        log.info("结算完成，用户ID：{}，外部交易单号：{}", userEntity.getUserId(), orderPaySuccessEntity.getOutTradeNo());

        return SettleOrderEntity.builder()
                .userId(userEntity.getUserId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(orderPaySuccessEntity.getOutTradeNo())
                .outTradeNoPayTime(orderPaySuccessEntity.getOutTradeNoPayTime())
                .source(orderPaySuccessEntity.getSource())
                .channel(orderPaySuccessEntity.getChannel())
                .isComplete(isComplete)
                .build();
    }

    @Override
    public List<NotifyTaskEntity> getUnNotifyTask() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.getUnNotifyTaskList();
        if (notifyTaskList == null || notifyTaskList.isEmpty()) {
            return null;
        }
        List<NotifyTaskEntity> notifyTaskEntityList = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {
            notifyTaskEntityList.add(NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build());
        }
        return notifyTaskEntityList;
    }

    @Override
    public List<NotifyTaskEntity> getUnNotifyTask(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.getUnNotifyTaskByTeamId(teamId);
        if (notifyTask == null) {
            return null;
        }
        return new ArrayList<>(){{
            add(NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build());
        }};
    }

    @Override
    public int updateNotifyTaskSuccess(String teamId) {
        return notifyTaskDao.updateNotifyTaskSuccess(teamId);
    }

    @Override
    public int updateNotifyTaskRetry(String teamId) {
        return notifyTaskDao.updateNotifyTaskRetry(teamId);
    }

    @Override
    public int updateNotifyTaskFail(String teamId) {
        return notifyTaskDao.updateNotifyTaskFail(teamId);
    }

}
