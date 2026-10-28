package groupbuy.market.plus.domain.trade.service.lock;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.common.Constants;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LockOrderServiceImpl implements LockOrderService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private BusinessLinkedList<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> lockOrderLink;

    @Override
    public LockOrderEntity lockOrder(UserEntity userEntity, GroupBuyTeamEntity groupBuyTeamEntity, OrderDetailEntity orderDetailEntity) throws Exception {
        // 按「活动 + 用户」加锁 - 参与次数、是否已在组队等校验为先查后写，并发锁单时需串行，避免突破参与次数限制
        // 锁覆盖责任链校验与锁单事务提交全过程；不等待，未获取到锁说明同一用户在该活动有锁单请求正在处理
        RLock lock = redissonClient.getLock(GroupBuyConstants.LockOrderUserLockPrefix + groupBuyTeamEntity.getActivityId() + Constants.UNDERSCORE + userEntity.getUserId());
        if (!lock.tryLock(0, TimeUnit.SECONDS)) {
            log.info("锁单请求处理中，拒绝重复请求，活动ID：{}，用户ID：{}", groupBuyTeamEntity.getActivityId(), userEntity.getUserId());
            throw new AppException(ResponseCodeEnum.E0031.getCode(), ResponseCodeEnum.E0031.getInfo());
        }
        try {
            return doLockOrder(userEntity, groupBuyTeamEntity, orderDetailEntity);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private LockOrderEntity doLockOrder(UserEntity userEntity, GroupBuyTeamEntity groupBuyTeamEntity, OrderDetailEntity orderDetailEntity) throws Exception {
        // 责任链过滤
        CheckLockEntity checkLockEntity = CheckLockEntity.builder()
                .activityId(groupBuyTeamEntity.getActivityId())
                .userId(userEntity.getUserId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .inviteId(userEntity.getInviteId())
                .build();
        CheckLockResEntity checkLockResEntity = lockOrderLink.apply(checkLockEntity, new LockOrderLinkFactory.DynamicContext());
        // 以责任链最终确定的组队为准（仅凭邀请码参团时由邀请码补全），保证 Redis 抢占与数据库落库为同一组队
        groupBuyTeamEntity.setTeamId(checkLockEntity.getTeamId());
        LockOrderAggregate orderAggregate = LockOrderAggregate.builder()
                .userEntity(userEntity)
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .orderDetailEntity(orderDetailEntity)
                .checkLockResEntity(checkLockResEntity)
                .build();
        try {
            // 尝试锁单
            return tradeRepository.lockOrder(orderAggregate);
        } catch (Exception e) {
            // 锁单失败时恢复可用位置
            Long recoverTeamStockCount = tradeRepository.recoverTeamStock(checkLockResEntity.getTeamStockRecoverKey());
            log.info("锁单失败 - 恢复可用位置，恢复Key：{}，当前恢复量：{}", checkLockResEntity.getTeamStockRecoverKey(), recoverTeamStockCount);
            throw e;
        }
    }

    @Override
    public LockOrderEntity getNoPayLockOrderByOutTradeNo(String userId, String outTradeNo) {
        return tradeRepository.getNoPayLockOrderByOutTradeNo(userId, outTradeNo);
    }

    @Override
    public TeamProgressVO getTeamProgress(String teamId) {
        return tradeRepository.getTeamProgress(teamId);
    }

}
