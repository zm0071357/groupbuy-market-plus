package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 退单类型枚举
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum RefundTypeEnum {

    TEAM_COMPLETE_PAID_REFUND(1, "teamCompletePaidRefund", "拼团组队完成 - 已支付") {
        @Override
        public boolean matches(OrderStatusEnum orderStatusEnum, TeamStatusEnum teamStatusEnum) {
            return orderStatusEnum.equals(OrderStatusEnum.COMPLETE) && teamStatusEnum.equals(TeamStatusEnum.COMPLETE);
        }
    },

    TEAM_INCOMPLETE_PAID_REFUND(2, "teamInCompletePaidRefund", "拼团组队未完成 - 已支付"){
        @Override
        public boolean matches(OrderStatusEnum orderStatusEnum, TeamStatusEnum teamStatusEnum) {
            return orderStatusEnum.equals(OrderStatusEnum.COMPLETE) && teamStatusEnum.equals(TeamStatusEnum.PROGRESS);
        }
    },

    TEAM_INCOMPLETE_UNPAID_REFUND(3, "teamInCompleteUnPaidRefund", "拼团组队未完成 - 未支付"){
        @Override
        public boolean matches(OrderStatusEnum orderStatusEnum, TeamStatusEnum teamStatusEnum) {
            return orderStatusEnum.equals(OrderStatusEnum.CREATE) && teamStatusEnum.equals(TeamStatusEnum.PROGRESS) ;
        }
    },
    ;
    private Integer type;

    private String strategy;

    private String info;

    /**
     * 匹配
     * @param orderStatusEnum 订单状态枚举
     * @param teamStatusEnum 拼团组队状态枚举
     * @return
     */
    public abstract boolean matches(OrderStatusEnum orderStatusEnum, TeamStatusEnum teamStatusEnum);

    /**
     * 根据类型获取枚举
     * @param type 类型
     * @return
     */
    public static RefundTypeEnum valueOf(Integer type) {
        switch (type) {
            case 1:
                return TEAM_COMPLETE_PAID_REFUND;
            case 2:
                return TEAM_INCOMPLETE_PAID_REFUND;
            case 3:
                return TEAM_INCOMPLETE_UNPAID_REFUND;
        }
        throw new AppException(ResponseCodeEnum.E0021.getCode(), ResponseCodeEnum.E0021.getInfo());
    }

    /**
     * 匹配获取枚举
     * @param orderStatusEnum 订单状态枚举
     * @param teamStatusEnum 拼团组队状态枚举
     * @return
     */
    public static RefundTypeEnum get(OrderStatusEnum orderStatusEnum, TeamStatusEnum teamStatusEnum) {
        return Arrays.stream(values())
                .filter(refundTypeEnum -> refundTypeEnum.matches(orderStatusEnum, teamStatusEnum))
                .findFirst()
                .orElseThrow(() -> new AppException(ResponseCodeEnum.E0021.getCode(), ResponseCodeEnum.E0021.getInfo()));
    }

}
