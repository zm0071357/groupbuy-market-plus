package groupbuy.market.plus.test.domain.tag;

import groupbuy.market.plus.domain.tag.service.TagService;
import groupbuy.market.plus.types.utils.ModuloUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 人群标签服务单元测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TagServiceTest {

    @Resource
    private TagService tagService;

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void test_crowdTagsJob() {
        tagService.execTagBatchJob("RQ_KJHKL98UU78H66554GFDV", "10001");
    }

    @Test
    public void test_getCrowdTagsUser() {
        RBitSet bitset = redissonClient.getBitSet("RQ_KJHKL98UU78H66554GFDV");
        log.info("399547479 是否在人群标签范围内：{}", bitset.get(ModuloUtil.getIndexFromUserId("399547479")));
        log.info("test 是否在人群标签范围内：{}", bitset.get(ModuloUtil.getIndexFromUserId("test")));
    }


}
