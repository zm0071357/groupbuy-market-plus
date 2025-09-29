package groupbuy.market.plus.domain.activity.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 商品详情页面 - 拼团组队信息实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexTeamEntity {

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 目标数量
     */
    private Integer targetCount;

    /**
     * 完成数量
     */
    private Integer completeCount;

    /**
     * 锁单数量
     */
    private Integer lockCount;

    /**
     * 拼团开始时间
     */
    private Date startTime;

    /**
     * 拼团结束时间
     */
    private Date endTime;

}
