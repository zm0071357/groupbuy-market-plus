package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.strategy.RefundService;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 执行退单节点
 */
@Slf4j
@Service
public class RefundOrderNode extends AbstractRefundOrderSupport<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> {

    @Resource
    private RefundEndNode refundEndNode;

    @Resource
    private Map<String, RefundService> refundServiceMap;

    @Override
    protected RefundResEntity doApply(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("退单服务 - 退单执行节点，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
        PreRefundOrderEntity preRefundOrderEntity = dynamicContext.getPreRefundOrderEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
        // 超时清单读取后，期限内支付的回执仍可能使组队成团。
        if (preRefundEntity.isTimeoutRefund() && !TeamStatusEnum.PROGRESS.equals(groupBuyTeamEntity.getTeamStatusEnum())) {
            dynamicContext.setRefundStatusEnum(RefundStatusEnum.SKIPPED);
            return router(preRefundEntity, dynamicContext);
        }
        // 拼单中退单的事务也会校验组队状态，防止读取状态后、写入前发生变化。
        // 根据订单状态和拼团状态获取对应策略
        RefundTypeEnum refundTypeEnum = RefundTypeEnum.get(preRefundOrderEntity.getOrderStatusEnum(), groupBuyTeamEntity.getTeamStatusEnum());
        log.info("退单服务 - 退单执行节点，用户ID：{}，外部交易单号：{}，退单类型：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo(), refundTypeEnum.getInfo());
        RefundService refundService = refundServiceMap.get(refundTypeEnum.getStrategy());
        // 执行退单
        log.info("退单服务 - 退单执行节点，执行退单开始，用户ID：{}，外部交易单号：{}，退单类型：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo(), refundTypeEnum.getInfo());
        refundService.refundOrder(RefundOrderEntity.builder()
                        .userId(preRefundEntity.getUserId())
                        .teamId(groupBuyTeamEntity.getTeamId())
                        .activityId(groupBuyTeamEntity.getActivityId())
                        .orderId(preRefundOrderEntity.getOrderId())
                        .outTradeNo(preRefundEntity.getOutTradeNo())
                        .timeoutRefund(preRefundEntity.isTimeoutRefund())
                .build());
        return router(preRefundEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        return refundEndNode;
    }

}
