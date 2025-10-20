package groupbuy.market.plus.infrastructure.adapter.repository;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundThreadTaskAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.*;
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
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String teamSuccessTopic;

    @Value("${spring.rabbitmq.config.producer.topic_order_refund.routing_key}")
    private String orderRefundTopic;

    @Value("${spring.rabbitmq.config.producer.topic_header_refund.routing_key}")
    private String headerRefundTopic;

    @Resource
    private RedissonClient redissonClient;

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

    @Override
    @Transactional(timeout = 500)
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
                    .notifyType(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().getType())
                    .notifyUrl(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.HTTP) ? groupBuyTeamEntity.getNotifyConfigVO().getNotifyUrl() : null)
                    .refundNotifyUrl(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.HTTP) ? groupBuyTeamEntity.getNotifyConfigVO().getNotifyUrl() : null)
                    .notifyMQ(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.MQ) ? teamSuccessTopic : null)
                    .refundNotifyMQ(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.MQ) ? orderRefundTopic : null)
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
                .originalPrice(orderDetailEntity.getOriginalPrice())
                .deductionPrice(orderDetailEntity.getDeductionPrice())
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
            // 最少支付0.01
            if (endPrice.compareTo(GroupBuyConstants.MinPrice) < 0) {
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
                .originalPrice(groupBuyTeamOrder.getOriginalPrice())
                .deductionPrice(groupBuyTeamOrder.getDeductionPrice())
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
                .teamStatusEnum(TeamStatusEnum.valueOf(groupBuyTeam.getStatus()))
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyTypeEnum(NotifyTypeEnum.getByType(groupBuyTeam.getNotifyType()))
                        .notifyUrl(groupBuyTeam.getNotifyUrl())
                        .notifyMQ(teamSuccessTopic)
                        .build())
                .build();
    }

    @Override
    public boolean isBlack(String resource, String channel) {
        return dccServiceImpl.isBlack(resource, channel);
    }

    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity settleOrder(SettleOrderAggregate settleOrderAggregate) {
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

        log.info("结算完成，用户ID：{}，外部交易单号：{}", userEntity.getUserId(), orderPaySuccessEntity.getOutTradeNo());

        // 最后一笔 - 拼团成功
        if (groupBuyTeamEntity.getTargetCount() - groupBuyTeamEntity.getCompleteCount() == 1) {
            log.info("拼团目标完成，组队ID：{}", groupBuyTeamEntity.getTeamId());
            // 更新拼团组队为完成
            Integer updateTeamStatusCount = groupBuyTeamDao.updateTeamStatusComplete(groupBuyTeamEntity.getTeamId());
            if (updateTeamStatusCount != 1) {
                throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
            }
            // 回调任务
            List<String> outTradeNoList = groupBuyTeamOrderDao.getCompleteTeamOutTradeNoList(groupBuyTeamEntity.getTeamId());
            NotifyTypeEnum notifyTypeEnum = groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum();
            String taskId = groupBuyTeamEntity.getTeamId() + Constants.UNDERSCORE + NotifyTaskTypeEnum.SETTLE.getType();
            NotifyTask notifyTask = NotifyTask.builder()
                    .activityId(groupBuyTeamEntity.getActivityId())
                    .teamId(groupBuyTeamEntity.getTeamId())
                    .taskId(taskId)
                    .taskType(NotifyTaskTypeEnum.SETTLE.getType())
                    .notifyType(notifyTypeEnum.getType())
                    .notifyUrl(notifyTypeEnum.equals(NotifyTypeEnum.HTTP) ? groupBuyTeamEntity.getNotifyConfigVO().getNotifyUrl() : null)
                    .notifyMQ(notifyTypeEnum.equals(NotifyTypeEnum.MQ) ? groupBuyTeamEntity.getNotifyConfigVO().getNotifyMQ() : null)
                    .notifyCount(0)
                    .notifyStatus(0)
                    .parameterJson(JSON.toJSONString(new HashMap<String, Object>(){{
                        put("teamId", groupBuyTeamEntity.getTeamId());
                        put("outTradeNoList", outTradeNoList);
                    }}))
                    .build();
            notifyTaskDao.insert(notifyTask);

            return NotifyTaskEntity.builder()
                    .taskId(taskId)
                    .teamId(notifyTask.getTeamId())
                    .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .build();
        }
        return null;
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
                    .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyMQ(teamSuccessTopic)
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
                    .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyMQ(teamSuccessTopic)
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

    @Override
    public boolean occupyTeamStock(Integer target, Integer validTime, String teamStockOccupyKey, String teamStockRecoverKey) {
        // 获取恢复量 - 没有恢复量时，get()方法会返回0
        // 恢复量：当拼团锁单最后失败，失败了需要把这个位置给恢复，其他用户可以抢占这个恢复位置，原来抢占失败的位置视为已经占有
        long teamStockRecoverCount = redissonClient.getAtomicLong(teamStockRecoverKey).get();
        // 抢占量+1，再加上团长原先就占有的一个位置即为目前的总抢占量
        long teamStockOccupyCount = redissonClient.getAtomicLong(teamStockOccupyKey).incrementAndGet() + 1;
        // 抢占量大于恢复量+目标量 - 位置已经不够了，返回失败
        if (teamStockOccupyCount > teamStockRecoverCount + target) {
            // 重置抢占量为目标量 - 位置已经不够，那抢占量最低就是目标量
            redissonClient.getAtomicLong(teamStockOccupyKey).set(target);
            return false;
        }
        // 给每个产生的值加锁兜底，虽然incr操作是原子的，基本不会产生一样的值
        // 但在实际生产中，遇到过集群的运维配置问题，以及业务运营配置数据问题，导致incr得到的值相同
        String lockKey = teamStockOccupyKey + Constants.UNDERSCORE + teamStockOccupyCount;
        boolean lock = redissonClient.getBucket(lockKey).trySet("lock", validTime + 60, TimeUnit.MINUTES);
        if (!lock) {
            log.info("拼团抢占可用位置加锁失败：{}", lockKey);
        }
        return lock;
    }

    @Override
    public Long recoverTeamStock(String teamStockRecoverKey) {
        // teamID为空 - 团长锁单失败，不需要恢复
        if (StringUtils.isBlank(teamStockRecoverKey)) {
            return 0L;
        }
        return redissonClient.getAtomicLong(teamStockRecoverKey).incrementAndGet();
    }

    @Override
    public RefundThreadTaskAggregate getRefundThreadTaskResAggregate(String userId, String outTradeNo) {
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(userId);
        groupBuyTeamOrderReq.setOutTradeNo(outTradeNo);
        GroupBuyTeamOrder groupBuyTeamOrder = groupBuyTeamOrderDao.getPreRefundOrder(groupBuyTeamOrderReq);
        if (groupBuyTeamOrder == null) {
            return null;
        }
        GroupBuyTeam groupBuyTeam = null;
        if (StringUtils.isNotBlank(groupBuyTeamOrder.getTeamId())) {
            groupBuyTeam = groupBuyTeamDao.getTeamById(groupBuyTeamOrder.getTeamId());
        }
        return RefundThreadTaskAggregate.builder()
                .preRefundOrderEntity(PreRefundOrderEntity.builder()
                        .userId(groupBuyTeamOrder.getUserId())
                        .teamId(groupBuyTeamOrder.getTeamId())
                        .orderId(groupBuyTeamOrder.getOrderId())
                        .isHeader(groupBuyTeamOrder.getIsHeader() == 1)
                        .orderStatusEnum(OrderStatusEnum.valueOf(groupBuyTeamOrder.getStatus()))
                        .build())
                .groupBuyTeamEntity(groupBuyTeam == null ? null : GroupBuyTeamEntity.builder()
                        .teamId(groupBuyTeam.getTeamId())
                        .activityId(groupBuyTeam.getActivityId())
                        .teamStatusEnum(TeamStatusEnum.valueOf(groupBuyTeam.getStatus()))
                        .notifyConfigVO(NotifyConfigVO.builder()
                                .notifyTypeEnum(NotifyTypeEnum.getByType(groupBuyTeam.getNotifyType()))
                                .refundNotifyUrl(groupBuyTeam.getRefundNotifyUrl())
                                .refundNotifyMQ(groupBuyTeam.getRefundNotifyMQ())
                                .headerRefundNotifyUrl(groupBuyTeam.getHeaderRefundNotifyUrl())
                                .headerRefundNotifyMQ(groupBuyTeam.getHeaderRefundNotifyMQ())
                                .build())
                        .build())
                .build();
    }

    @Override
    public String getNewHeaderUser(String teamId) {
        // 获取新团长ID
        String newHeaderUserId = groupBuyTeamOrderDao.getNewHeaderUserId(teamId);
        // 更新为新团长
        if (StringUtils.isNotBlank(newHeaderUserId)) {
            Integer updateCount = groupBuyTeamOrderDao.updateUserIsHeader(newHeaderUserId);
            if (updateCount != 1) {
                throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
            }
        }
        return newHeaderUserId;
    }

    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity teamInCompleteUnPaidRefund(RefundOrderAggregate teamInCompleteUnPaidRefundAggregate) {
        // 更新订单状态为退单
        RefundOrderEntity refundOrderEntity = teamInCompleteUnPaidRefundAggregate.getRefundOrderEntity();
        TeamProgressVO teamProgressVO = teamInCompleteUnPaidRefundAggregate.getTeamProgressVO();
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(refundOrderEntity.getOrderId());
        groupBuyTeamOrderReq.setOrderId(refundOrderEntity.getOrderId());
        Integer updateOrderCount = groupBuyTeamOrderDao.updateOrderStatusRefund(groupBuyTeamOrderReq);
        if (updateOrderCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 更新拼团组队 - 锁单量-1
        GroupBuyTeam groupBuyTeamReq = new GroupBuyTeam();
        groupBuyTeamReq.setLockCount(teamProgressVO.getLockCount());
        groupBuyTeamReq.setTeamId(refundOrderEntity.getTeamId());
        Integer updateTeamCount = groupBuyTeamDao.updateTeamByTeamInCompleteUnPaidRefund(groupBuyTeamReq);
        if (updateTeamCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 回调任务
        String taskId = refundOrderEntity.getTeamId() + Constants.UNDERSCORE +
                NotifyTaskTypeEnum.TEAM_INCOMPLETE_UNPAID_REFUND.getType() + Constants.UNDERSCORE +
                refundOrderEntity.getOrderId();
        NotifyTask notifyTask = NotifyTask.builder()
                .activityId(refundOrderEntity.getActivityId())
                .teamId(refundOrderEntity.getTeamId())
                .taskId(taskId)
                .taskType(NotifyTaskTypeEnum.TEAM_INCOMPLETE_UNPAID_REFUND.getType())
                .notifyType(NotifyTypeEnum.MQ.getType())
                .notifyMQ(orderRefundTopic)
                .notifyStatus(0)
                .notifyCount(0)
                .parameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                    put("type", NotifyTaskTypeEnum.TEAM_INCOMPLETE_UNPAID_REFUND.getType());
                    put("userId", refundOrderEntity.getUserId());
                    put("teamId", refundOrderEntity.getTeamId());
                    put("orderId", refundOrderEntity.getOrderId());
                    put("activityId", refundOrderEntity.getActivityId());
                }}))
                .build();
        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(notifyTask.getTeamId())
                .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity teamInCompletePaidRefund(RefundOrderAggregate teamInCompletePaidRefundAggregate) {
        // 更新订单状态为退单
        RefundOrderEntity refundOrderEntity = teamInCompletePaidRefundAggregate.getRefundOrderEntity();
        TeamProgressVO teamProgressVO = teamInCompletePaidRefundAggregate.getTeamProgressVO();
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(refundOrderEntity.getUserId());
        groupBuyTeamOrderReq.setOrderId(refundOrderEntity.getOrderId());
        Integer updateOrderCount = groupBuyTeamOrderDao.updateOrderStatusRefund(groupBuyTeamOrderReq);
        if (updateOrderCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 更新拼团组队 - 锁单量-1，完成量-1
        GroupBuyTeam groupBuyTeamReq = new GroupBuyTeam();
        groupBuyTeamReq.setLockCount(teamProgressVO.getLockCount());
        groupBuyTeamReq.setCompleteCount(teamProgressVO.getCompleteCount());
        groupBuyTeamReq.setTeamId(refundOrderEntity.getTeamId());
        Integer updateTeamCount = groupBuyTeamDao.updateTeamByTeamInCompletePaidRefund(groupBuyTeamReq);
        if (updateTeamCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 回调任务
        String taskId = refundOrderEntity.getTeamId() + Constants.UNDERSCORE +
                NotifyTaskTypeEnum.TEAM_INCOMPLETE_PAID_REFUND.getType() + Constants.UNDERSCORE +
                refundOrderEntity.getOrderId();
        NotifyTask notifyTask = NotifyTask.builder()
                .taskId(taskId)
                .activityId(refundOrderEntity.getActivityId())
                .teamId(refundOrderEntity.getTeamId())
                .taskType(NotifyTaskTypeEnum.TEAM_INCOMPLETE_PAID_REFUND.getType())
                .notifyType(NotifyTypeEnum.MQ.getType())
                .notifyMQ(orderRefundTopic)
                .notifyStatus(0)
                .notifyCount(0)
                .parameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                    put("type", NotifyTaskTypeEnum.TEAM_INCOMPLETE_PAID_REFUND.getType());
                    put("userId", refundOrderEntity.getUserId());
                    put("teamId", refundOrderEntity.getTeamId());
                    put("orderId", refundOrderEntity.getOrderId());
                    put("activityId", refundOrderEntity.getActivityId());
                }}))
                .build();
        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(notifyTask.getTeamId())
                .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity teamCompletePaidRefund(RefundOrderAggregate teamCompletePaidRefundAggregate) {
        // 更新订单状态为退单
        RefundOrderEntity refundOrderEntity = teamCompletePaidRefundAggregate.getRefundOrderEntity();
        TeamProgressVO teamProgressVO = teamCompletePaidRefundAggregate.getTeamProgressVO();
        TeamStatusEnum teamStatusEnum = teamCompletePaidRefundAggregate.getTeamStatusEnum();
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setUserId(refundOrderEntity.getOrderId());
        groupBuyTeamOrderReq.setOrderId(refundOrderEntity.getOrderId());
        Integer updateOrderCount = groupBuyTeamOrderDao.updateOrderStatusRefund(groupBuyTeamOrderReq);
        if (updateOrderCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 更新拼团组队 - 锁单量-1，完成量-1，拼团组队状态
        GroupBuyTeam groupBuyTeamReq = new GroupBuyTeam();
        groupBuyTeamReq.setLockCount(teamProgressVO.getLockCount());
        groupBuyTeamReq.setCompleteCount(teamProgressVO.getCompleteCount());
        groupBuyTeamReq.setStatus(teamStatusEnum.getStatus());
        groupBuyTeamReq.setTeamId(refundOrderEntity.getTeamId());
        Integer updateTeamCount = groupBuyTeamDao.updateTeamByTeamCompletePaidRefund(groupBuyTeamReq);
        if (updateTeamCount != 1) {
            throw new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo());
        }

        // 回调任务
        String taskId = refundOrderEntity.getTeamId() + Constants.UNDERSCORE +
                NotifyTaskTypeEnum.TEAM_COMPLETE_PAID_REFUND.getType() + Constants.UNDERSCORE +
                refundOrderEntity.getOrderId();
        NotifyTask notifyTask = NotifyTask.builder()
                .taskId(taskId)
                .activityId(refundOrderEntity.getActivityId())
                .teamId(refundOrderEntity.getTeamId())
                .taskType(NotifyTaskTypeEnum.TEAM_COMPLETE_PAID_REFUND.getType())
                .notifyType(NotifyTypeEnum.MQ.getType())
                .notifyMQ(orderRefundTopic)
                .notifyStatus(0)
                .notifyCount(0)
                .parameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                    put("type", NotifyTaskTypeEnum.TEAM_COMPLETE_PAID_REFUND.getType());
                    put("userId", refundOrderEntity.getUserId());
                    put("teamId", refundOrderEntity.getTeamId());
                    put("orderId", refundOrderEntity.getOrderId());
                    put("activityId", refundOrderEntity.getActivityId());
                }}))
                .build();
        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(notifyTask.getTeamId())
                .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

    @Override
    public NotifyTaskEntity newHeaderNotify(String newLeaderUserId, String orderId, GroupBuyTeamEntity groupBuyTeamEntity) {
        // 回调任务
        String taskId = groupBuyTeamEntity.getTeamId() + Constants.UNDERSCORE +
                NotifyTaskTypeEnum.NEW_HEADER.getType() + Constants.UNDERSCORE +
                orderId;
        NotifyTask notifyTask = NotifyTask.builder()
                .taskId(taskId)
                .activityId(groupBuyTeamEntity.getActivityId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .taskType(NotifyTaskTypeEnum.NEW_HEADER.getType())
                .notifyType(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().getType())
                .notifyUrl(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.HTTP) ? groupBuyTeamEntity.getNotifyConfigVO().getHeaderRefundNotifyUrl() : null)
                .notifyMQ(groupBuyTeamEntity.getNotifyConfigVO().getNotifyTypeEnum().equals(NotifyTypeEnum.MQ) ? headerRefundTopic : null)
                .notifyStatus(0)
                .notifyCount(0)
                .parameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                    put("type", NotifyTaskTypeEnum.NEW_HEADER.getType());
                    put("userId", newLeaderUserId);
                    put("teamId", groupBuyTeamEntity.getTeamId());
                    put("orderId", orderId);
                    put("activityId", groupBuyTeamEntity.getActivityId());
                    put("teamStatus", groupBuyTeamEntity.getTeamStatusEnum().getStatus());
                }}))
                .build();
        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(notifyTask.getTeamId())
                .notifyTypeEnum(NotifyTypeEnum.getByType(notifyTask.getNotifyType()))
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

}
