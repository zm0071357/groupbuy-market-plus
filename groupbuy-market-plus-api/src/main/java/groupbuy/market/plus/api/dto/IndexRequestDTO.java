package groupbuy.market.plus.api.dto;

import lombok.Getter;

/**
 * 商品页面请求体
 */
@Getter
public class IndexRequestDTO {


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

}
