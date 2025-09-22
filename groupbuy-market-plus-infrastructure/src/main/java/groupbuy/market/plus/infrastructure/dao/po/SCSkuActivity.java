package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
