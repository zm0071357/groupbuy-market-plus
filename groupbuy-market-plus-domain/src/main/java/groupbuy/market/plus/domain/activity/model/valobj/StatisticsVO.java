package groupbuy.market.plus.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品详情页面 - 统计值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsVO {

    /**
     * 拼团数量
     */
    private Integer allTeamCount;

    /**
     * 拼团完成数量
     */
    private Integer allTeamCompleteCount;

    /**
     * 参与拼团总人数
     */
    private Integer allTeamUserCount;

}
