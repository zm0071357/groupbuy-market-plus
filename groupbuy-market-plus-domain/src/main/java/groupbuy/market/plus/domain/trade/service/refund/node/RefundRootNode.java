package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
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
public class RefundRootNode extends AbstractRefundOrderSupport<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> {

    @Resource
    private RefundRepeatNode refundRepeatNode;

    @Override
    protected RefundResEntity doApply(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("退单服务 - 根节点，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
        // 参数校验
        if (StringUtils.isBlank(preRefundEntity.getUserId()) || StringUtils.isBlank(preRefundEntity.getOutTradeNo()) ||
                StringUtils.isBlank(preRefundEntity.getSource()) || StringUtils.isBlank(preRefundEntity.getChannel())) {
            throw new AppException(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo());
        }
        return router(preRefundEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        return refundRepeatNode;
    }

}
