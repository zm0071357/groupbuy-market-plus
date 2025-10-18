package groupbuy.market.plus.domain.trade.model.aggregate;

import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单聚合
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderAggregate {

    /**
     * 用户信息
     */
    private UserEntity userEntity;

    /**
     * 退单返回结果
     */
    private RefundResEntity refundResEntity;
}
