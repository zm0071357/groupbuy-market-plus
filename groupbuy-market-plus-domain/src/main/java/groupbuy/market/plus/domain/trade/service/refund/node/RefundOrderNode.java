package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.strategy.RefundService;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
                .build());
        // 新团长回调
        if (StringUtils.isNotBlank(dynamicContext.getNewLeaderUserId())) {
            log.info("退单服务 - 退单执行节点，新团长回调，新团长用户ID：{}", dynamicContext.getNewLeaderUserId());
            refundService.newHeaderNotify(dynamicContext.getNewLeaderUserId(), preRefundOrderEntity.getOrderId(), groupBuyTeamEntity);
        }
        return router(preRefundEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        return refundEndNode;
    }

}
