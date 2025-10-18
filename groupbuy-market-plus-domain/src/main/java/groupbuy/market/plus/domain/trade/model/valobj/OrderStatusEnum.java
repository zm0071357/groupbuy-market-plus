package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum OrderStatusEnum {

    CREATE(0, "初始创建"),
    COMPLETE(1, "支付完成"),
    REFUND(2, "用户退单"),
    CLOSE(3, "超时关单"),
    ;

    private Integer code;
    private String info;

    public static OrderStatusEnum valueOf(Integer code) {
        switch (code) {
            case 0:
                return CREATE;
            case 1:
                return COMPLETE;
            case 2:
                return REFUND;
            case 3:
                return CLOSE;
            default:
                throw new AppException(ResponseCodeEnum.E0022.getCode(), ResponseCodeEnum.E0022.getInfo());
        }

    }

}
