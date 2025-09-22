package groupbuy.market.plus.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品值信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 原始价格
     */
    private BigDecimal originalPrice;

}
