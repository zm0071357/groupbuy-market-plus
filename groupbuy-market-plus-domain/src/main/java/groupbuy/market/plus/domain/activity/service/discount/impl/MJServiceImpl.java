package groupbuy.market.plus.domain.activity.service.discount.impl;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.discount.AbstractDiscountService;
import groupbuy.market.plus.types.common.Constants;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 满减
 */
@Slf4j
@Service("MJ")
public class MJServiceImpl extends AbstractDiscountService {

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        String marketExpr = groupBuyDiscount.getMarketExpr();
        String[] split = marketExpr.split(Constants.SPLIT);
        // 满减需要的价格
        BigDecimal reqPrice = new BigDecimal(split[0]);
        // 扣减的价格
        BigDecimal deductionPrice = new BigDecimal(split[1]);
        // 不满足满减条件 - 返回原始价格
        if (originalPrice.compareTo(reqPrice) < 0) {
            return originalPrice;
        }
        BigDecimal payPrice = originalPrice.subtract(deductionPrice);
        // 最低价格不能低于0.01元
        if (payPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return GroupBuyConstants.MinPrice;
        }
        return payPrice;
    }
}
