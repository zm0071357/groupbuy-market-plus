package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 人群标签-用户关联
 * 人群标签和用户是一对多的关系
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrowdTagsUser {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 人群ID
     */
    private String tagId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
