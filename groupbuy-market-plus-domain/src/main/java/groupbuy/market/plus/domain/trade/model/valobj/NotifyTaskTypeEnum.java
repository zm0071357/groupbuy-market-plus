package groupbuy.market.plus.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyTaskTypeEnum {

    SETTLE(1, "成团结算回调"),
    TEAM_INCOMPLETE_UNPAID_REFUND(2, "退单 - 拼团组队未完成 - 未支付回调"),
    TEAM_INCOMPLETE_PAID_REFUND(3, "退单 - 拼团组队未完成 - 已支付回调"),
    TEAM_COMPLETE_PAID_REFUND(4, "退单 - 拼团组队完成 - 已支付回调"),
    NEW_HEADER(5, "新团长回调"),
    ;
    private Integer type;
    private String info;

}
