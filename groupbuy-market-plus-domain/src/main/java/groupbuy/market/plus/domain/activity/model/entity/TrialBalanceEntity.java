package groupbuy.market.plus.domain.activity.model.entity;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 对营销商品进行试算的结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceEntity {

    /**
     * 商品 ID
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

    /**
     * 优惠价格
     */
    private BigDecimal deductionPrice;

    /**
     * 支付价格
     */
    private BigDecimal payPrice;

    /**
     * 拼团活动信息
     */
    private ActivityVO activityVO;

    /**
     * 是否可见拼团
     */
    private Boolean isVisible;

    /**
     * 是否可参与拼团
     */
    private Boolean isEnable;

}
