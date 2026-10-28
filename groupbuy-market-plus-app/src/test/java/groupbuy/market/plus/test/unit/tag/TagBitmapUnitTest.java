package groupbuy.market.plus.test.unit.tag;

import groupbuy.market.plus.infrastructure.adapter.repository.TagRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.CrowdTagsUserDao;
import groupbuy.market.plus.infrastructure.dao.po.CrowdTagsUser;
import groupbuy.market.plus.types.utils.ModuloUtil;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 人群标签 bitmap 单元测试 - 下标范围、key 版本、重复用户仍写入 bitmap
 */
public class TagBitmapUnitTest {

    @Test
    public void test_index_range() {
        for (int i = 0; i < 10000; i++) {
            int index = ModuloUtil.getIndexFromUserId("user" + i);
            Assert.assertTrue(index >= 0 && index < (1 << 26));
        }
    }

    @Test
    public void test_addUser_duplicateStillWriteBitmap_versionedKey() {
        CrowdTagsUserDao crowdTagsUserDao = mock(CrowdTagsUserDao.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBitSet bitSet = mock(RBitSet.class);
        when(redissonClient.getBitSet("RQ_KJHKL98UU78H66554GFDV_v2")).thenReturn(bitSet);
        // 第二个用户在库中已存在
        doNothing().doThrow(new DuplicateKeyException("dup")).when(crowdTagsUserDao).addCrowdTagsUser(any(CrowdTagsUser.class));
        TagRepositoryImpl tagRepository = new TagRepositoryImpl();
        ReflectionTestUtils.setField(tagRepository, "crowdTagsUserDao", crowdTagsUserDao);
        ReflectionTestUtils.setField(tagRepository, "redissonClient", redissonClient);

        tagRepository.addCrowdTagsUser("RQ_KJHKL98UU78H66554GFDV", Arrays.asList("user01", "user02"));

        verify(bitSet).set(ModuloUtil.getIndexFromUserId("user01"), true);
        verify(bitSet).set(ModuloUtil.getIndexFromUserId("user02"), true);
        verify(redissonClient, never()).getBitSet("RQ_KJHKL98UU78H66554GFDV");
    }

    @Test
    public void test_isTagCrowdUser_versionedKey() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBitSet bitSet = mock(RBitSet.class);
        when(redissonClient.getBitSet("RQ_1_v2")).thenReturn(bitSet);
        when(bitSet.isExists()).thenReturn(true);
        when(bitSet.get(ModuloUtil.getIndexFromUserId("user01"))).thenReturn(true);
        TagRepositoryImpl tagRepository = new TagRepositoryImpl();
        ReflectionTestUtils.setField(tagRepository, "redissonClient", redissonClient);

        Assert.assertTrue(tagRepository.isTagCrowdUser("RQ_1", "user01"));
        Assert.assertFalse(tagRepository.isTagCrowdUser("RQ_1", "user02"));
    }

}
