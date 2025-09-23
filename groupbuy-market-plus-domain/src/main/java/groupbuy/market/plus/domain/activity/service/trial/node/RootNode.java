package groupbuy.market.plus.domain.activity.service.trial.node;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 根节点
 */
@Slf4j
@Service
public class RootNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private SwitchNode switchNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品试算服务-根节点 userId：{}", marketProductEntity.getUserId());
        if (StringUtils.isBlank(marketProductEntity.getUserId()) ||
                StringUtils.isBlank(marketProductEntity.getGoodsId()) ||
                StringUtils.isBlank(marketProductEntity.getSource()) ||
                StringUtils.isBlank(marketProductEntity.getChannel())) {
            throw new AppException(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo());
        }
        return router(marketProductEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return switchNode;
    }
}
