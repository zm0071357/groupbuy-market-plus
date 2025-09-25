package groupbuy.market.plus.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.management.loading.MLetContent;

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
    INDEX_EXCEPTION("0003", "唯一索引冲突"),
    E0001("E0001", "不存在对应的折扣计算服务"),
    E0002("E0002", "不存在拼团营销配置"),
    E0003("E0003", "服务降级拦截"),
    E0004("E0004", "服务切量拦截"),
    E0005("E0005", "不存在对应的活动状态"),
    E0006("E0006", "活动状态非有效"),
    E0007("E0007", "活动不在有效期内"),
    E0008("E0008", "用户参与活动次数达到上限"),
    E0009("E0009", "用户不可参与同一个组队"),
    E0010("E0010", "不存在的活动"),
    E0011("E0011", "更新锁单量失败"),
    E0012("E0012", "加入失败，当前拼团人数已满"),
    ;
    private String code;
    private String info;
}
