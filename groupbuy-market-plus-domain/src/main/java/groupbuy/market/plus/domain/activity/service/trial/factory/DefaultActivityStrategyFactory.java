package groupbuy.market.plus.domain.activity.service.trial.factory;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.trial.node.RootNode;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 规则树工厂
 * 初始化根节点（串联整个链路）、定义上下文信息
 */
@Service
public class DefaultActivityStrategyFactory {

    private final RootNode rootNode;

    public DefaultActivityStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * 从根节点开始串联链路
     * @return
     */
    public StrategyHandler<MarketProductEntity, DynamicContext, TrialBalanceEntity> strategyHandler() {
        return rootNode;
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
         * 拼团商品
         */
        private SkuVO skuVO;

        /**
         * 拼团活动
         */
        private ActivityVO activityVO;

        /**
         * 是否可以进行优惠试算
         */
        private boolean isDiscount;

        /**
         * 优惠价格
         */
        private BigDecimal deductionPrice;

        /**
         * 支付价格
         */
        private BigDecimal payPrice;

    }

}
