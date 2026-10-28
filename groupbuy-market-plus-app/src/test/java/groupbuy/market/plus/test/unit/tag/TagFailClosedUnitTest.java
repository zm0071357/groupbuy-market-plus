package groupbuy.market.plus.test.unit.tag;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.DiscountTypeEnum;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.discount.impl.ZJServiceImpl;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.node.TagNode;
import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import groupbuy.market.plus.infrastructure.adapter.repository.TagRepositoryImpl;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 人群标签数据缺失时拒绝优惠单元测试
 */
public class TagFailClosedUnitTest {

    private static final String TAG_ID = "RQ_1";

    @Test
    public void test_bitmapMissing_notInTag() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBitSet bitSet = mock(RBitSet.class);
        when(redissonClient.getBitSet(TAG_ID + "_v2")).thenReturn(bitSet);
        when(bitSet.isExists()).thenReturn(false);
        TagRepositoryImpl tagRepository = new TagRepositoryImpl();
        ReflectionTestUtils.setField(tagRepository, "redissonClient", redissonClient);

        Assert.assertFalse(tagRepository.isTagCrowdUser(TAG_ID, "user01"));
        Assert.assertFalse(tagRepository.isTagCrowdExists(TAG_ID));
    }

    @Test
    public void test_tagDiscount_bitmapMissing_originalPrice() {
        TagRepository tagRepository = mock(TagRepository.class);
        when(tagRepository.isTagCrowdUser(TAG_ID, "user01")).thenReturn(false);
        ZJServiceImpl discountService = new ZJServiceImpl();
        ReflectionTestUtils.setField(discountService, "tagRepository", tagRepository);

        BigDecimal price = discountService.calculate("user01", new BigDecimal("100"), ActivityVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.TAG).tagId(TAG_ID).marketExpr("20").build());

        Assert.assertEquals(0, new BigDecimal("100").compareTo(price));
    }

    @Test
    public void test_tagNode_bitmapMissing_rejectWhitelistAndBlacklist() throws Exception {
        // tagScope "2,2" 白名单：仅人群内用户享受优惠；"1,1" 黑名单：人群内用户原价、人群外用户享受优惠
        for (String tagScope : new String[]{"2,2", "1,1"}) {
            DefaultActivityStrategyFactory.DynamicContext context = runTagNode(tagScope);
            Assert.assertFalse("tagScope=" + tagScope + " 标签数据缺失时不可享用优惠", context.isDiscount());
        }
    }

    private DefaultActivityStrategyFactory.DynamicContext runTagNode(String tagScope) throws Exception {
        TagRepository tagRepository = mock(TagRepository.class);
        when(tagRepository.isTagCrowdExists(TAG_ID)).thenReturn(false);
        TagNode tagNode = new TagNode();
        ReflectionTestUtils.setField(tagNode, "tagRepository", tagRepository);
        DefaultActivityStrategyFactory.DynamicContext context = new DefaultActivityStrategyFactory.DynamicContext();
        context.setActivityVO(ActivityVO.builder().activityId(100123L).tagId(TAG_ID).tagScope(tagScope).build());
        context.setSkuVO(SkuVO.builder().goodsId("9890001").originalPrice(new BigDecimal("100")).build());
        Method doApply = TagNode.class.getDeclaredMethod("doApply", MarketProductEntity.class, DefaultActivityStrategyFactory.DynamicContext.class);
        doApply.setAccessible(true);
        try {
            doApply.invoke(tagNode, MarketProductEntity.builder().userId("user01").goodsId("9890001").build(), context);
        } catch (InvocationTargetException ignored) {
            // 路由到下一节点时未注入依赖，忽略；只关心上下文中的优惠判定
        }
        verify(tagRepository, never()).isTagCrowdUser(anyString(), anyString());
        return context;
    }

}
