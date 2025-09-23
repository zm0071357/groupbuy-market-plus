package groupbuy.market.plus.domain.tag.adapter.repository;

import groupbuy.market.plus.domain.tag.model.entity.CrowdTagsJobEntity;

import java.util.List;

public interface TagRepository {

    /**
     * 获取人群标签任务
     * @param tagId 人群标签ID
     * @param batchId 批次ID
     * @return
     */
    CrowdTagsJobEntity getCrowdTagsJob(String tagId, String batchId);

    /**
     * 新增人群标签-用户关联
     * @param tagId 人群标签ID
     * @param userIdList 用户ID集合
     */
    void addCrowdTagsUser(String tagId, List<String> userIdList);

    /**
     * 更新人群标签统计量
     * @param tagId 人群标签ID
     * @param size 新增量
     */
    void updateCrowdTagsStatistics(String tagId, int size);

    /**
     * 判断用户是否在人群标签范围内
     * @param tagId 人群标签ID
     * @param userId 用户ID
     * @return
     */
    boolean isTagCrowdUser(String tagId, String userId);
}
