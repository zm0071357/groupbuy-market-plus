package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 商品-活动关联
 * 活动和商品是一对多的关系
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivity {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 活动ID
     */
    private Long activityId;

    /** 老带新资格所属店铺，空表示不限新老客。 */
    private String eligibilityShopId;

    /** 活动配置的免拼实付价，空表示未启用。 */
    private BigDecimal skipGroupPrice;

    /**
     * 商品ID
     */
    private String goodsId;

    /** 可选拼团档位 JSON，NULL 表示普通拼团。 */
    private String tiersJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
