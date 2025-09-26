package groupbuy.market.plus.domain.trade.model.valobj;

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
    RETURN(2, "用户退单"),
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
                return CLOSE;
        }
        return CREATE;
    }

}
