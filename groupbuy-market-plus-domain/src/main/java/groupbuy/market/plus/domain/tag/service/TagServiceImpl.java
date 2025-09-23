package groupbuy.market.plus.domain.tag.service;

import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import groupbuy.market.plus.domain.tag.model.entity.CrowdTagsJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TagServiceImpl implements TagService{

    @Resource
    private TagRepository tagRepository;

    @Override
    public void execTagBatchJob(String tagId, String batchId) {
        log.info("人群标签批次任务 tagId:{} batchId:{}", tagId, batchId);
        CrowdTagsJobEntity crowdTagsJobEntity = tagRepository.getCrowdTagsJob(tagId, batchId);
        List<String> userIdList = new ArrayList<>() {{
            add("399547479");
            add("137158130");
            add("134137257");
        }};
        tagRepository.addCrowdTagsUser(tagId, userIdList);
        tagRepository.updateCrowdTagsStatistics(tagId, userIdList.size());
    }
}
