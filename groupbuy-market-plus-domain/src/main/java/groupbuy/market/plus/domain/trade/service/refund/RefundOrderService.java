package groupbuy.market.plus.domain.trade.service.refund;

import groupbuy.market.plus.domain.trade.model.entity.*;

public interface RefundOrderService {

    /**
     * 退单
     * @param preRefundEntity
     * @return
     */
    RefundResEntity refundOrder(PreRefundEntity preRefundEntity) throws Exception;

}
