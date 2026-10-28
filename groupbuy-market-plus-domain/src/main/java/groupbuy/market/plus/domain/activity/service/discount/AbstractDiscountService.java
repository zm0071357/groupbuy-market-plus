package groupbuy.market.plus.domain.activity.service.discount;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.DiscountTypeEnum;
import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠计算抽象类
 */
@Slf4j
public abstract class AbstractDiscountService implements DisCountService{

    @Resource
    private TagRepository tagRepository;

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount) {
        // 折扣是否限定人群标签
        if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())) {
            // 校验
            boolean isCrowRange = filterTagId(groupBuyDiscount.getTagId(), userId);
            if (!isCrowRange) {
                return originalPrice;
            }
        }
        return normalizePrice(originalPrice, doCalculate(originalPrice, groupBuyDiscount));
    }

    /**
     * 优惠后价格统一收尾：保留两位小数（四舍五入），最低 0.01 元，且不高于原价
     * @param originalPrice 原价
     * @param payPrice 优惠计算得到的价格
     * @return 收尾后的支付价格
     */
    private BigDecimal normalizePrice(BigDecimal originalPrice, BigDecimal payPrice) {
        BigDecimal price = payPrice.setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(GroupBuyConstants.MinPrice) < 0) {
            price = GroupBuyConstants.MinPrice;
        }
        return price.min(originalPrice);
    }

    private boolean filterTagId(String tagId, String userId) {
        return tagRepository.isTagCrowdUser(tagId, userId);
    }

    /**
     * 计算优惠
     * @param originalPrice
     * @param groupBuyDiscount
     * @return
     */
    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, ActivityVO.GroupBuyDiscount groupBuyDiscount);
}
