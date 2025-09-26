package groupbuy.market.plus.domain.trade.service.settle;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class SettleOrderServiceImpl implements SettleOrderService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private BusinessLinkedList<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> settleOrderLink;

    @Override
    public SettleOrderEntity settleOrder(OrderPaySuccessEntity orderPaySuccessEntity) throws Exception {
        // 责任链过滤
        CheckSettleEntity checkSettleEntity = CheckSettleEntity.builder()
                .userId(orderPaySuccessEntity.getUserId())
                .resource(orderPaySuccessEntity.getSource())
                .channel(orderPaySuccessEntity.getChannel())
                .outTradeNo(orderPaySuccessEntity.getOutTradeNo())
                .outTradeNoPayTime(orderPaySuccessEntity.getOutTradeNoPayTime())
                .build();
        CheckSettleResEntity checkSettleResEntity = settleOrderLink.apply(checkSettleEntity, new SettleOrderLinkFactory.DynamicContext());
        SettleOrderAggregate settleOrderAggregate = SettleOrderAggregate.builder()
                .userEntity(UserEntity.builder().userId(orderPaySuccessEntity.getUserId()).build())
                .groupBuyTeamEntity(GroupBuyTeamEntity.builder()
                        .teamId(checkSettleResEntity.getTeamId())
                        .activityId(checkSettleResEntity.getActivityId())
                        .targetCount(checkSettleResEntity.getTargetCount())
                        .completeCount(checkSettleResEntity.getCompleteCount())
                        .lockCount(checkSettleResEntity.getLockCount())
                        .status(checkSettleResEntity.getStatus().getCode())
                        .startTime(checkSettleResEntity.getStartTime())
                        .endTime(checkSettleResEntity.getEndTime())
                        .build())
                .orderPaySuccessEntity(orderPaySuccessEntity)
                .build();
        return tradeRepository.settleOrder(settleOrderAggregate);
    }

}
