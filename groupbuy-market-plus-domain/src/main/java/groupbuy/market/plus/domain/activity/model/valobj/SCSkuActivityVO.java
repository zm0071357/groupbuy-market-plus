package groupbuy.market.plus.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品活动配置值对象
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivityVO {

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String chanel;

    /**
     * 活动ID
     */
    private Long activityId;

    /** 活动配置的免拼实付价，空表示未启用。 */
    private BigDecimal skipGroupPrice;

    /**
     * 商品ID
     */
    private String goodsId;

    /** 可选拼团档位，空集合表示普通拼团。 */
    @Builder.Default
    private List<GroupBuyTierVO> tiers = List.of();

}
