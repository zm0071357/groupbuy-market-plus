package groupbuy.market.plus.domain.trade.service.refund.strategy.impl;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.service.refund.strategy.AbstractRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 退单
 * 拼团组队未完成 - 未支付
 */
@Slf4j
@Service("teamInCompleteUnPaidRefund")
public class TeamInCompleteUnPaidRefund extends AbstractRefundService {

    @Override
    public void refundOrder(RefundOrderEntity refundOrderEntity) {
        log.info("退单 - 拼团组队未完成 - 未支付：{}", JSON.toJSONString(refundOrderEntity));
    }
}
