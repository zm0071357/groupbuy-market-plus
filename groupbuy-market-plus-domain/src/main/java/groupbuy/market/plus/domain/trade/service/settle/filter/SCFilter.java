package groupbuy.market.plus.domain.trade.service.settle.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleResEntity;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * SC黑名单过滤节点 - SC不为黑名单才能结算
 */
@Slf4j
@Service
public class SCFilter implements LogicHandler<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckSettleResEntity apply(CheckSettleEntity checkSettleEntity, SettleOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入结算责任链 - SC黑名单过滤节点");
        // 校验SC黑名单
        log.info("结算责任链 - SC黑名单过滤节点，SC黑名单校验，source：{}，channel：{}", checkSettleEntity.getSource(), checkSettleEntity.getChannel());
        if (tradeRepository.isBlack(checkSettleEntity.getSource(), checkSettleEntity.getChannel())) {
            throw new AppException(ResponseCodeEnum.E0017.getCode(), ResponseCodeEnum.E0017.getInfo());
        }
        return next(checkSettleEntity, dynamicContext);
    }
}
