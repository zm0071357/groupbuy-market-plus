package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 拼团组队状态枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TeamStatusEnum {
    PROGRESS(0, "拼单中"),
    COMPLETE(1, "完成"),
    FAIL(2, "失败"),;

    private Integer code;
    private String info;

    public static TeamStatusEnum valueOf(Integer code) {
        switch (code) {
            case 0:
                return PROGRESS;
            case 1:
                return COMPLETE;
            case 2:
                return FAIL;
        }
        throw new AppException(ResponseCodeEnum.E0018.getCode(), ResponseCodeEnum.E0018.getInfo());
    }
}
