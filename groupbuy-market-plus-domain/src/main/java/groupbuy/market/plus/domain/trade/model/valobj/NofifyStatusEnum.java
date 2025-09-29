package groupbuy.market.plus.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 回调状态枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NofifyStatusEnum {

    SUCCESS("SUCCESS", "回调成功"),
    FAIL("FAIL", "回调失败"),
    NULL("NULL", "空执行");

    private String status;
    private String info;
}
