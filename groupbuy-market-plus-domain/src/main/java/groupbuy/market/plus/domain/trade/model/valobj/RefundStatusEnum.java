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
    SKIPPED("skipped", "组队已结束，跳过超时退单"),
    FAIL("fail", "失败"),
    ;

    private String code;
    private String info;

}
