package groupbuy.market.plus.domain.tag.service;

public interface TagService {

    /**
     * 执行人群标签任务
     * @param tagId 人群标签ID
     * @param batchId 批次ID
     */
    void execTagBatchJob(String tagId, String batchId);

}
