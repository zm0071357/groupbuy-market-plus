package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 活动状态枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ActivityStatusEnum {
    CREATE(0, "创建"),
    EFFECTIVE(1, "生效"),
    OVERDUE(2, "过期"),
    ABANDONED(3, "废弃"),
    ;

    private Integer code;
    private String info;

    public static ActivityStatusEnum valueOf(Integer code) {
        switch (code) {
            case 0:
                return CREATE;
            case 1:
                return EFFECTIVE;
            case 2:
                return OVERDUE;
            case 3:
                return ABANDONED;
        }
        throw new AppException(ResponseCodeEnum.E0005.getCode(), ResponseCodeEnum.E0005.getInfo());
    }
}
