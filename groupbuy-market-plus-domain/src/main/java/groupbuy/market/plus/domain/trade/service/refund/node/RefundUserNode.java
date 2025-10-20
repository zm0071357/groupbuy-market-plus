package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 退单成员节点
 */
@Slf4j
@Service
public class RefundUserNode extends AbstractRefundOrderSupport<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> {

    @Resource
    private RefundOrderNode refundOrderNode;

    @Override
    protected RefundResEntity doApply(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("退单服务 - 退单成员节点，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
        PreRefundOrderEntity preRefundOrderEntity = dynamicContext.getPreRefundOrderEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
        if (preRefundOrderEntity.getIsHeader()) {
            log.info("退单服务 - 退单成员节点，团长进行退单，用户ID：{}", preRefundEntity.getUserId());
            // 组队未完成 - 选出新团长
            if (groupBuyTeamEntity.getTeamStatusEnum().equals(TeamStatusEnum.PROGRESS)) {
                String newHeaderUserId = tradeRepository.getNewHeaderUser(groupBuyTeamEntity.getTeamId());
                // 写入动态上下文
                dynamicContext.setNewLeaderUserId(newHeaderUserId);
                log.info("退单服务 - 退单成员节点，拼团组队未完成，选出新团长，新团长ID：{}", newHeaderUserId);
            } else {
                log.info("退单服务 - 退单成员节点，拼团组队已完成，无需选出新团长，用户ID：{}", preRefundEntity.getUserId());
            }
        } else {
            log.info("退单服务 - 退单成员节点，团员进行退单，用户ID：{}", preRefundEntity.getUserId());
        }
        return router(preRefundEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        return refundOrderNode;
    }
}
