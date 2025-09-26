package groupbuy.market.plus.domain.trade.service.settle.factory;

import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleResEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.service.settle.filter.OrderStatusFilter;
import groupbuy.market.plus.domain.trade.service.settle.filter.OrderTimeFilter;
import groupbuy.market.plus.domain.trade.service.settle.filter.SCFilter;
import groupbuy.market.plus.types.design.framework.link.multition.LinkArmory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 结算责任链工厂
 */
@Service
public class SettleOrderLinkFactory {

    @Bean("settleOrderLink")
    public BusinessLinkedList<CheckSettleEntity, DynamicContext, CheckSettleResEntity> settleOrderLink(SCFilter scFilter, OrderStatusFilter orderStatusFilter, OrderTimeFilter orderTimeFilter) {
        // 组装链
        LinkArmory<CheckSettleEntity, DynamicContext, CheckSettleResEntity> linkArmory = new LinkArmory<>("结算责任链", scFilter, orderStatusFilter, orderTimeFilter);
        return linkArmory.getLogicLink();
    }

    /**
     * 动态上下文信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /**
         * 锁单订单
         */
        private LockOrderEntity lockOrderEntity;
    }
}
