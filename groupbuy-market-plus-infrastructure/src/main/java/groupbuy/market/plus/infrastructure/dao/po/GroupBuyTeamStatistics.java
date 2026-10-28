package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团组队统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyTeamStatistics {

    /**
     * 组队总数
     */
    private Integer allTeamCount;

    /**
     * 完成组队数
     */
    private Integer allTeamCompleteCount;

    /**
     * 完成组队的参团人数
     */
    private Integer allTeamUserCount;

}
