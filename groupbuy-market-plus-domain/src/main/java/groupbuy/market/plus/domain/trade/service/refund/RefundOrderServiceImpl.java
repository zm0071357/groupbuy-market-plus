package groupbuy.market.plus.domain.trade.service.refund;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.strategy.RefundService;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Service
public class RefundOrderServiceImpl implements RefundOrderService {

    @Resource
    private DefaultRefundStrategyFactory defaultRefundStrategyFactory;

    @Resource
    private Map<String, RefundService> refundServiceMap;

    @Override
    public RefundResEntity refundOrder(PreRefundEntity preRefundEntity) throws Exception {
        StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> strategyHandler = defaultRefundStrategyFactory.strategyHandler();
        RefundResEntity refundResEntity = strategyHandler.apply(preRefundEntity, new DefaultRefundStrategyFactory.DynamicContext());
        log.info("用户ID：{}，外部交易单号：{}，退单结果：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo(), JSON.toJSONString(refundResEntity));
        return refundResEntity;
    }

    @Override
    public void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception {
        // 根据退单类型获取对应退单服务
        RefundTypeEnum refundTypeEnum = RefundTypeEnum.valueOf(orderRefundMessage.getType());
        RefundService refundService = refundServiceMap.get(refundTypeEnum.getStrategy());
        // 恢复拼团组队库存
        refundService.recoverTeamLockStock(orderRefundMessage);
    }

}
