package groupbuy.market.plus.domain.activity.service.discount.impl;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.discount.AbstractDiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 直减
 */
@Slf4j
@Service("ZJ")
public class ZJServiceImpl extends AbstractDiscountService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // 直减
        BigDecimal deductPrice = new BigDecimal(marketExpr);
        // 最低价格、保留位数由抽象类统一收尾
        return originalPrice.subtract(deductPrice);
    }
}
