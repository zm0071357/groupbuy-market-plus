package groupbuy.market.plus.domain.trade.service.refund.factory;

import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.node.RefundRootNode;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * 规则树工厂
 */
@Service
public class DefaultRefundStrategyFactory {

    private final RefundRootNode refundRootNode;

    public DefaultRefundStrategyFactory(RefundRootNode refundRootNode) {
        this.refundRootNode = refundRootNode;
    }

    public StrategyHandler<PreRefundEntity, DynamicContext, RefundResEntity> strategyHandler() {
        return refundRootNode;
    }

    /**
     * 动态上下文 - 用于节点间传输数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicContext {

        /**
         * 预支付订单信息
         */
        private PreRefundOrderEntity preRefundOrderEntity;

        /**
         * 拼团组队信息
         */
        private GroupBuyTeamEntity groupBuyTeamEntity;

        /**
         * 新团长用户ID
         */
        private String newLeaderUserId;

        /**
         * 退单状态枚举
         */
        private RefundStatusEnum refundStatusEnum;
    }
}

