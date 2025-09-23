package groupbuy.market.plus.domain.activity.service.discount;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;

import java.math.BigDecimal;

public interface DisCountService {

    /**
     * 折扣计算
     * @param userId 用户ID
     * @param originalPrice 商品原始价格
     * @param groupBuyDiscount 折扣配置
     * @return 商品优惠价格
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount);
}
