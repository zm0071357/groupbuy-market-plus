package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 邀请码状态枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum InviteStatusEnum {

    AVAILABLE(0, "可用"),
    UNAVAILABLE(1, "不可用"),
    ;

    private Integer status;

    private String info;

    public static InviteStatusEnum valueOf(Integer status) {
        switch (status){
            case 0:
                return AVAILABLE;
            case 1:
                return UNAVAILABLE;
            default:
                throw new AppException(ResponseCodeEnum.E0024.getCode(), ResponseCodeEnum.E0024.getInfo());
        }
    }
}
