package groupbuy.market.plus.domain.trade.service.lock;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class LockOrderServiceImpl implements LockOrderService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private BusinessLinkedList<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> lockOrderLink;

    @Override
    public LockOrderEntity lockOrder(UserEntity userEntity, GroupBuyTeamEntity groupBuyTeamEntity, OrderDetailEntity orderDetailEntity) throws Exception {
        // 责任链过滤
        CheckLockEntity checkLockEntity = CheckLockEntity.builder()
                .activityId(groupBuyTeamEntity.getActivityId())
                .userId(userEntity.getUserId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .build();
        CheckLockResEntity checkLockResEntity = lockOrderLink.apply(checkLockEntity, new LockOrderLinkFactory.DynamicContext());
        LockOrderAggregate orderAggregate = LockOrderAggregate.builder()
                .userEntity(userEntity)
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .orderDetailEntity(orderDetailEntity)
                .checkLockResEntity(checkLockResEntity)
                .build();
        return tradeRepository.lockOrder(orderAggregate);
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
