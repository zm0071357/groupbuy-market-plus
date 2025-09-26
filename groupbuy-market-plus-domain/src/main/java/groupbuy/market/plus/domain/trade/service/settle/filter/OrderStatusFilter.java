package groupbuy.market.plus.domain.trade.service.settle.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckSettleResEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 订单状态过滤节点 - 订单状态需支付完成
 */
@Slf4j
@Service
public class OrderStatusFilter implements LogicHandler<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckSettleResEntity apply(CheckSettleEntity checkSettleEntity, SettleOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入结算责任链 - 订单过滤节点");
        LockOrderEntity lockOrderEntity = tradeRepository.checkLockOrderStatusByOutTradeNo(checkSettleEntity.getUserId(), checkSettleEntity.getOutTradeNo());
        if (lockOrderEntity == null) {
            throw new AppException(ResponseCodeEnum.E0013.getCode(), ResponseCodeEnum.E0013.getInfo());
        }
        // 校验订单状态
        log.info("结算责任链 - 订单过滤节点，订单状态校验，订单ID：{}，外部交易单号：{}", lockOrderEntity.getOrderId(), checkSettleEntity.getOutTradeNo());
        OrderStatusEnum orderStatusEnum = lockOrderEntity.getOrderStatusEnum();
        if (!orderStatusEnum.equals(OrderStatusEnum.CREATE)) {
            throw new AppException(ResponseCodeEnum.E0014.getCode(), ResponseCodeEnum.E0014.getInfo().concat(orderStatusEnum.getInfo()));
        }
        dynamicContext.setLockOrderEntity(lockOrderEntity);
        return next(checkSettleEntity, dynamicContext);
    }
}
