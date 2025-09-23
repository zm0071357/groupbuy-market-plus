package groupbuy.market.plus.domain.activity.service.discount.impl;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.discount.AbstractDiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * N元购
 */
@Slf4j
@Service("N")
public class NServiceImpl extends AbstractDiscountService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        String marketExpr = groupBuyDiscount.getMarketExpr();
        return new BigDecimal(marketExpr);
    }
}
