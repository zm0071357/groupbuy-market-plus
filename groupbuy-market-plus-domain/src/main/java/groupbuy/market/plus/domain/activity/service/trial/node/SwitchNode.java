package groupbuy.market.plus.domain.activity.service.trial.node;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 开关节点
 */
@Slf4j
@Service
public class SwitchNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private MarketNode marketNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品试算服务-开关节点 userId：{}", marketProductEntity.getUserId());
        return router(marketProductEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return marketNode;
    }
}
