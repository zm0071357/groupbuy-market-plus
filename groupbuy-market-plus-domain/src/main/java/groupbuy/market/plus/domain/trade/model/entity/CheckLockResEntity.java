package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁单校验结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckLockResEntity {

    /**
     * 是否为团长
     */
    private Boolean isHeader;

}
