package groupbuy.market.plus.domain.activity.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 自选档位的编号、真实成团人数及团员实付价（元）。 */
public record GroupBuyTierVO(Long tierId, Integer targetCount, BigDecimal memberPrice) {

    /** 校验商品档位配置并按人数升序返回，保证人数越多团员价越低。 */
    public static List<GroupBuyTierVO> validate(List<GroupBuyTierVO> tiers, BigDecimal originalPrice) {
        if (tiers == null || tiers.isEmpty()) {
            return List.of();
        }

        Set<Long> tierIds = new HashSet<>();
        for (GroupBuyTierVO tier : tiers) {
            if (tier == null || tier.tierId() == null || tier.tierId() <= 0 || !tierIds.add(tier.tierId())
                    || tier.targetCount() == null || tier.targetCount() < 2
                    || !GroupBuyPrice.isValidOffer(tier.memberPrice(), originalPrice)) {
                throw new AppException(ResponseCodeEnum.E0036.getCode(), ResponseCodeEnum.E0036.getInfo());
            }
        }

        List<GroupBuyTierVO> sortedTiers = tiers.stream()
                .sorted(Comparator.comparing(GroupBuyTierVO::targetCount))
                .toList();
        for (int index = 1; index < sortedTiers.size(); index++) {
            GroupBuyTierVO previous = sortedTiers.get(index - 1);
            GroupBuyTierVO current = sortedTiers.get(index);
            if (current.targetCount().equals(previous.targetCount())
                    || current.memberPrice().compareTo(previous.memberPrice()) >= 0) {
                throw new AppException(ResponseCodeEnum.E0036.getCode(), ResponseCodeEnum.E0036.getInfo());
            }
        }
        return sortedTiers.stream()
                .map(tier -> new GroupBuyTierVO(tier.tierId(), tier.targetCount(), tier.memberPrice().setScale(2)))
                .toList();
    }
}
