package groupbuy.market.plus.infrastructure.adapter.repository;

import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import groupbuy.market.plus.domain.tag.model.entity.CrowdTagsJobEntity;
import groupbuy.market.plus.infrastructure.dao.CrowdTagsDao;
import groupbuy.market.plus.infrastructure.dao.CrowdTagsJobDao;
import groupbuy.market.plus.infrastructure.dao.CrowdTagsUserDao;
import groupbuy.market.plus.infrastructure.dao.po.CrowdTags;
import groupbuy.market.plus.infrastructure.dao.po.CrowdTagsJob;
import groupbuy.market.plus.infrastructure.dao.po.CrowdTagsUser;
import groupbuy.market.plus.types.utils.ModuloUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class TagRepositoryImpl implements TagRepository {

    /**
     * 人群标签 bitmap key 版本后缀 - bitmap 下标范围调整后与旧数据隔离，避免新旧下标混用误判
     */
    private static final String BITMAP_KEY_VERSION = "_v2";

    @Resource
    private CrowdTagsDao crowdTagsDao;

    @Resource
    private CrowdTagsUserDao crowdTagsUserDao;

    @Resource
    private CrowdTagsJobDao crowdTagsJobDao;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public CrowdTagsJobEntity getCrowdTagsJob(String tagId, String batchId) {
        CrowdTagsJob crowdTagsJobReq = new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);
        CrowdTagsJob crowdTagsJob = crowdTagsJobDao.getCrowdTagsJob(crowdTagsJobReq);
        if (crowdTagsJob == null) {
            return null;
        }
        return CrowdTagsJobEntity.builder()
                .tagType(crowdTagsJob.getTagType())
                .tagRule(crowdTagsJob.getTagRule())
                .startTime(crowdTagsJob.getStartTime())
                .endTime(crowdTagsJob.getEndTime())
                .build();
    }

    @Override
    public void addCrowdTagsUser(String tagId, List<String> userIdList) {
        for (String userId : userIdList) {
            CrowdTagsUser crowdTagsUserReq = new CrowdTagsUser();
            crowdTagsUserReq.setTagId(tagId);
            crowdTagsUserReq.setUserId(userId);
            try {
                crowdTagsUserDao.addCrowdTagsUser(crowdTagsUserReq);
            } catch (DuplicateKeyException ignore) {
                log.info("tagId：{}，userId：{} 人群标签用户已存在", tagId, userId);
            }
            // bitmap 写入为幂等操作，用户已存在时同样写入，保证重跑标签任务可重建 bitmap
            redissonClient.getBitSet(getBitmapKey(tagId)).set(ModuloUtil.getIndexFromUserId(userId), true);
            log.info("tagId：{}，userId：{} 写入bitmap完成", tagId, userId);
        }
    }

    @Override
    public void updateCrowdTagsStatistics(String tagId, int size) {
        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(size);
        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);
    }

    @Override
    public boolean isTagCrowdUser(String tagId, String userId) {
        RBitSet bitSet = redissonClient.getBitSet(getBitmapKey(tagId));
        if (!bitSet.isExists()) {
            return true;
        }
        return bitSet.get(ModuloUtil.getIndexFromUserId(userId));
    }

    private String getBitmapKey(String tagId) {
        return tagId + BITMAP_KEY_VERSION;
    }

}
