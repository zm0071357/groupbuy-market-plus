package groupbuy.market.plus.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团组队进度值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProgressVO {

    /**
     * 目标量
     */
    private Integer targetCount;

    /**
     * 完成量
     */
    private Integer completeCount;

    /**
     * 锁单量
     */
    private Integer lockCount;

}
