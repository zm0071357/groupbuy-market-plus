package groupbuy.market.plus.domain.activity.service.discount.impl;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.discount.AbstractDiscountService;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 折扣
 */
@Slf4j
@Service("ZK")
public class ZKServiceImpl extends AbstractDiscountService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // 折扣
        BigDecimal discount = new BigDecimal(marketExpr);
        BigDecimal payPrice = originalPrice.multiply(discount);
        if (payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return GroupBuyConstants.MinPrice;
        }
        return payPrice;
    }
}
