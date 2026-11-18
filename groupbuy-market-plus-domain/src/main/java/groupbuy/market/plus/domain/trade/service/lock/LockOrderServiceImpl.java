package groupbuy.market.plus.domain.trade.service.lock;

import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyPrice;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.model.valobj.SkipGroupBalanceVO;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.common.Constants;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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
        // 只有无组队、无邀请码的请求才是开团，可依据当前活动配置选择档位。
        if (Boolean.TRUE.equals(groupBuyTeamEntity.getSkipGroup())) {
            prepareSkipGroup(userEntity, groupBuyTeamEntity, orderDetailEntity);
        } else if (StringUtils.isBlank(groupBuyTeamEntity.getTeamId()) && StringUtils.isBlank(userEntity.getInviteId())) {
            applyOpeningTier(groupBuyTeamEntity, orderDetailEntity);
        }
        // 责任链过滤
        CheckLockEntity checkLockEntity = CheckLockEntity.builder()
                .activityId(groupBuyTeamEntity.getActivityId())
                .goodsId(orderDetailEntity.getGoodsId())
                .source(orderDetailEntity.getSource())
                .channel(orderDetailEntity.getChannel())
                .userId(userEntity.getUserId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .tierId(groupBuyTeamEntity.getTierId())
                .inviteId(userEntity.getInviteId())
                .build();
        CheckLockResEntity checkLockResEntity = lockOrderLink.apply(checkLockEntity, new LockOrderLinkFactory.DynamicContext());
        // 以责任链最终确定的组队为准（仅凭邀请码参团时由邀请码补全），保证 Redis 抢占与数据库落库为同一组队
        groupBuyTeamEntity.setTeamId(checkLockEntity.getTeamId());
        try {
            GroupBuyTeamEntity joinedTeam = checkLockResEntity.getJoinedTeam();
            if (joinedTeam != null && joinedTeam.getTierId() != null) {
                // 参团使用开团时保存的金额和组队时段，活动改价后旧团仍按原价参团。
                orderDetailEntity.setOriginalPrice(joinedTeam.getOriginalPrice());
                orderDetailEntity.setPayPrice(joinedTeam.getMemberPrice());
                orderDetailEntity.setDeductionPrice(joinedTeam.getOriginalPrice().subtract(joinedTeam.getMemberPrice()));
                groupBuyTeamEntity.setStartTime(joinedTeam.getStartTime());
                groupBuyTeamEntity.setEndTime(joinedTeam.getEndTime());
            }
            return tradeRepository.lockOrder(LockOrderAggregate.builder()
                    .userEntity(userEntity)
                    .groupBuyTeamEntity(groupBuyTeamEntity)
                    .orderDetailEntity(orderDetailEntity)
                    .checkLockResEntity(checkLockResEntity)
                    .build());
        } catch (Exception e) {
            // 锁单失败时恢复可用位置
            Long recoverTeamStockCount = tradeRepository.recoverTeamStock(checkLockResEntity.getTeamStockRecoverKey(), groupBuyTeamEntity.getValidTime());
            log.info("锁单失败 - 恢复可用位置，恢复Key：{}，当前恢复量：{}", checkLockResEntity.getTeamStockRecoverKey(), recoverTeamStockCount);
            throw e;
        }
    }

    private void prepareSkipGroup(UserEntity user, GroupBuyTeamEntity team, OrderDetailEntity order) {
        if (StringUtils.isNotBlank(team.getTeamId()) || StringUtils.isNotBlank(user.getInviteId()) || team.getTierId() != null) {
            throw new AppException(ResponseCodeEnum.E0046.getCode(), ResponseCodeEnum.E0046.getInfo());
        }
        if (!GroupBuyPrice.isValidOffer(order.getSkipGroupPrice(), order.getOriginalPrice())) {
            throw new AppException(ResponseCodeEnum.E0044.getCode(), ResponseCodeEnum.E0044.getInfo());
        }
        team.setTargetCount(1);
        order.setPayPrice(order.getSkipGroupPrice().setScale(2));
        order.setDeductionPrice(order.getOriginalPrice().subtract(order.getPayPrice()));
    }

    @Override
    public SkipGroupBalanceVO getSkipGroupBalance(String userId, String source, String channel) {
        return tradeRepository.getSkipGroupBalance(userId, source, channel);
    }

    private void applyOpeningTier(GroupBuyTeamEntity team, OrderDetailEntity order) {
        List<GroupBuyTierVO> tiers = GroupBuyTierVO.validate(order.getTiers(), order.getOriginalPrice());
        if (tiers.isEmpty() && team.getTierId() == null) {
            return;
        }
        GroupBuyTierVO tier = tiers.stream()
                .filter(candidate -> candidate.tierId().equals(team.getTierId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCodeEnum.E0035.getCode(), ResponseCodeEnum.E0035.getInfo()));

        team.setTargetCount(tier.targetCount());
        order.setPayPrice(tier.memberPrice());
        order.setDeductionPrice(order.getOriginalPrice().subtract(tier.memberPrice()));
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
