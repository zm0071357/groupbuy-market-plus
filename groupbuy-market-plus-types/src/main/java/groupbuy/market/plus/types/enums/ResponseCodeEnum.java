package groupbuy.market.plus.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 返回码枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ResponseCodeEnum {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    E0001("E0001", "不存在对应的折扣计算服务"),
    E0002("E0002", "不存在拼团营销配置"),
    E0003("E0003", "服务降级拦截"),
    E0004("E0004", "服务切量拦截"),
    ;

    private String code;
    private String info;
}
