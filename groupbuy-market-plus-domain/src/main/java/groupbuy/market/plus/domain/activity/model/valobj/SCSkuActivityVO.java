package groupbuy.market.plus.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /**
     * 商品ID
     */
    private String goodsId;

}
