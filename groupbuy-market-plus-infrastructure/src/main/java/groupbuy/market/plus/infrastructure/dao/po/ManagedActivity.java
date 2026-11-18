package groupbuy.market.plus.infrastructure.dao.po;

import lombok.Data;

/** 独立保存运营草稿及发布后的活动编号，避免草稿进入交易查询。 */
@Data
public class ManagedActivity {
    private Long id;
    private Long activityId;
    private String state;
    private String draftJson;
}
