package groupbuy.market.plus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderResponseDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

    /**
     * 退单状态码
     */
    private String refundCode;

    /**
     * 退单状态信息
     */
    private String refundInfo;
}
