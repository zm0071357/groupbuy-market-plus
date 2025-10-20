package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 回调类型枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyTypeEnum {

    HTTP(1,"HTTP"),
    MQ(2,"MQ"),
    ;
    private Integer notifyCode;
    private String type;

    /**
     * 根据类型获取枚举
     * @param notifyCode
     * @return
     */
    public static NotifyTypeEnum getByNotifyCode(Integer notifyCode) {
        switch (notifyCode) {
            case 1:
                return HTTP;
            case 2:
                return MQ;
            default:
                throw new AppException(ResponseCodeEnum.E0019.getCode(), ResponseCodeEnum.E0019.getInfo());
        }
    }

    /**
     * 根据类型获取枚举
     * @param type 类型
     * @return
     */
    public static NotifyTypeEnum getByType(String type) {
        switch (type) {
            case "HTTP":
                return HTTP;
            case "MQ":
                return MQ;
            default:
                throw new AppException(ResponseCodeEnum.E0019.getCode(), ResponseCodeEnum.E0019.getInfo());
        }
    }

}
