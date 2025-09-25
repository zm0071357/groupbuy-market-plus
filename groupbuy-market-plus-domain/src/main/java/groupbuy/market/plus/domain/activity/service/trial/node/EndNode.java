package groupbuy.market.plus.domain.activity.service.trial.node;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 尾节点
 */
@Slf4j
@Service
public class EndNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品试算服务-尾节点 userId：{}", marketProductEntity.getUserId());
        return TrialBalanceEntity.builder()
                .goodsId(marketProductEntity.getGoodsId())
                .originalPrice(dynamicContext.getSkuVO().getOriginalPrice())
                .deductionPrice(dynamicContext.getDeductionPrice() == null ? BigDecimal.ZERO : dynamicContext.getDeductionPrice())
                .payPrice(dynamicContext.getPayPrice() == null ? dynamicContext.getSkuVO().getOriginalPrice() : dynamicContext.getPayPrice())
                .activityVO(dynamicContext.getActivityVO())
                .build();
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return defaultStrategyHandler;
    }

}
