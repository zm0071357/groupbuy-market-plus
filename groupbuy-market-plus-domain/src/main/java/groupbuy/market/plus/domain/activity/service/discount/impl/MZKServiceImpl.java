package groupbuy.market.plus.domain.activity.service.discount.impl;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.discount.AbstractDiscountService;
import groupbuy.market.plus.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 满折扣
 */
@Slf4j
@Service("MZK")
public class MZKServiceImpl extends AbstractDiscountService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        String marketExpr = groupBuyDiscount.getMarketExpr();
        String[] split = marketExpr.split(Constants.SPLIT);
        // 满折扣需要的价格
        BigDecimal reqPrice = new BigDecimal(split[0]);
        // 折扣
        BigDecimal discount = new BigDecimal(split[1]);
        if (originalPrice.compareTo(reqPrice) < 0) {
            return originalPrice;
        }
        BigDecimal deductPrice = originalPrice.multiply(discount);
        if (deductPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }
        return deductPrice;
    }
}
