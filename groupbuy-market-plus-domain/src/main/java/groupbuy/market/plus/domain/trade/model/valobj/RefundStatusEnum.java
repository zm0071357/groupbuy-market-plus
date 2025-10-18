package groupbuy.market.plus.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum RefundStatusEnum {

    SUCCESS("success", "成功"),
    REPEAT("repeat", "重复"),
    FAIL("fail", "失败"),
    ;

    private String code;
    private String info;

}
