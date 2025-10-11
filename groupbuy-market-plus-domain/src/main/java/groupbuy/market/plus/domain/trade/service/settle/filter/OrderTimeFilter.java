package groupbuy.market.plus.domain.trade.service.settle.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 订单支付时间过滤节点 - 订单支付时间需要在拼团有效时间范围内
 */
@Slf4j
@Service
public class OrderTimeFilter implements LogicHandler<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckSettleResEntity apply(CheckSettleEntity checkSettleEntity, SettleOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入结算责任链 - 订单支付时间过滤节点");
        GroupBuyTeamEntity groupBuyTeamEntity = tradeRepository.getTeamById(dynamicContext.getLockOrderEntity().getTeamId());
        if (groupBuyTeamEntity == null) {
            throw new AppException(ResponseCodeEnum.E0015.getCode(), ResponseCodeEnum.E0015.getInfo());
        }
        // 校验支付时间
        log.info("结算责任链 - 订单支付时间过滤节点，支付时间校验，订单ID：{}，外部交易单号：{}", dynamicContext.getLockOrderEntity().getOrderId(), checkSettleEntity.getOutTradeNo());
        Date outTradeNoPayTime = checkSettleEntity.getOutTradeNoPayTime();
        if (outTradeNoPayTime.before(groupBuyTeamEntity.getStartTime()) || outTradeNoPayTime.after(groupBuyTeamEntity.getEndTime())) {
            throw new AppException(ResponseCodeEnum.E0016.getCode(), ResponseCodeEnum.E0016.getInfo());
        }
        return CheckSettleResEntity.builder()
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .targetCount(groupBuyTeamEntity.getTargetCount())
                .completeCount(groupBuyTeamEntity.getCompleteCount())
                .lockCount(groupBuyTeamEntity.getLockCount())
                .status(TeamStatusEnum.valueOf(groupBuyTeamEntity.getStatus()))
                .startTime(groupBuyTeamEntity.getStartTime())
                .endTime(groupBuyTeamEntity.getEndTime())
                .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
                .build();
    }
}
