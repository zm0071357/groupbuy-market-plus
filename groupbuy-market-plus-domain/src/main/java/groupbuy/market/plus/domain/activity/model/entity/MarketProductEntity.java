package groupbuy.market.plus.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 营销商品
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketProductEntity {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 商品 ID
     */
    private String goodId;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

}
