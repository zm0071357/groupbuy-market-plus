package groupbuy.market.plus.domain.activity.service.discount;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.DiscountTypeEnum;
import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;

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
        return doCalculate(originalPrice, groupBuyDiscount);
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
