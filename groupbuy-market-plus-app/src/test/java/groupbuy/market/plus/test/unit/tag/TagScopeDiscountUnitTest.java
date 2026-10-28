package groupbuy.market.plus.test.unit.tag;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.node.MarketNode;
import groupbuy.market.plus.domain.activity.service.trial.node.TagNode;
import groupbuy.market.plus.domain.activity.service.trial.node.TrialEndNode;
import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 人群范围优惠判定单元测试 - 人群标签只决定能否享受优惠（不限制参与拼团）
 * 2,2 白名单：仅人群内用户享受优惠；1,1 黑名单：人群内用户（如羊毛党）原价，人群外用户享受优惠；
 * 其他取值或未配置范围视为配置错误，所有用户均不享受优惠
 */
public class TagScopeDiscountUnitTest {

    private static final String TAG_ID = "RQ_KJHKL98UU78H66554GFDV";

    @Test
    public void test_whiteList_onlyCrowdUserDiscount() throws Exception {
        Assert.assertTrue("白名单 - 人群内用户享受优惠", discount("2,2", true));
        Assert.assertFalse("白名单 - 人群外用户原价", discount("2,2", false));
    }

    @Test
    public void test_blackList_crowdUserOriginalPrice() throws Exception {
        Assert.assertFalse("黑名单 - 人群内用户（羊毛党）原价", discount("1,1", true));
        Assert.assertTrue("黑名单 - 人群外用户享受优惠", discount("1,1", false));
    }

    @Test
    public void test_scopeWithWhitespace_normalize() throws Exception {
        Assert.assertFalse(discount(" 1, 1 ", true));
        Assert.assertTrue(discount(" 1, 1 ", false));
    }

    @Test
    public void test_invalidScope_noDiscountAndNoQuery() throws Exception {
        // 混合取值、非法取值、未配置范围 - 配置错误，人群内外均不享受优惠，也不再查询人群数据
        for (String invalid : new String[]{"1,2", "2,1", "1", "2", "3,3", "1,1,1", "", " ", null}) {
            for (boolean isTagCrowdUser : new boolean[]{true, false}) {
                TagRepository tagRepository = mock(TagRepository.class);
                Assert.assertFalse("tagScope=[" + invalid + "] 应不享受优惠", runTagNode(tagRepository, invalid, isTagCrowdUser).isDiscount());
                verify(tagRepository, never()).isTagCrowdExists(anyString());
                verify(tagRepository, never()).isTagCrowdUser(anyString(), anyString());
            }
        }
    }

    @Test
    public void test_noTag_allDiscount() throws Exception {
        TagRepository tagRepository = mock(TagRepository.class);
        DefaultActivityStrategyFactory.DynamicContext context = new DefaultActivityStrategyFactory.DynamicContext();
        context.setActivityVO(ActivityVO.builder().activityId(100123L).tagId(null).tagScope(null).build());
        context.setSkuVO(SkuVO.builder().goodsId("9890001").originalPrice(new BigDecimal("100")).build());

        invoke(tagRepository, context);

        // 未配置人群标签 - 所有用户享受优惠，人群范围不参与判断
        Assert.assertTrue(context.isDiscount());
        verifyNoInteractions(tagRepository);
    }

    @Test
    public void test_activityVO_rule() {
        Assert.assertTrue(ActivityVO.builder().tagScope("2,2").build().isValidTagScope());
        Assert.assertTrue(ActivityVO.builder().tagScope("1,1").build().isValidTagScope());
        Assert.assertFalse(ActivityVO.builder().tagScope("1,2").build().isValidTagScope());
        Assert.assertFalse(ActivityVO.builder().tagScope(null).build().isValidTagScope());
        Assert.assertTrue(ActivityVO.builder().tagScope("2,2").build().isTagCrowdDiscount(true));
        Assert.assertFalse(ActivityVO.builder().tagScope("2,2").build().isTagCrowdDiscount(false));
        Assert.assertFalse(ActivityVO.builder().tagScope("1,1").build().isTagCrowdDiscount(true));
        Assert.assertTrue(ActivityVO.builder().tagScope("1,1").build().isTagCrowdDiscount(false));
    }

    private boolean discount(String tagScope, boolean isTagCrowdUser) throws Exception {
        return runTagNode(mock(TagRepository.class), tagScope, isTagCrowdUser).isDiscount();
    }

    private DefaultActivityStrategyFactory.DynamicContext runTagNode(TagRepository tagRepository, String tagScope, boolean isTagCrowdUser) throws Exception {
        when(tagRepository.isTagCrowdExists(TAG_ID)).thenReturn(true);
        when(tagRepository.isTagCrowdUser(TAG_ID, "user01")).thenReturn(isTagCrowdUser);
        DefaultActivityStrategyFactory.DynamicContext context = new DefaultActivityStrategyFactory.DynamicContext();
        context.setActivityVO(ActivityVO.builder().activityId(100123L).tagId(TAG_ID).tagScope(tagScope).build());
        context.setSkuVO(SkuVO.builder().goodsId("9890001").originalPrice(new BigDecimal("100")).build());
        invoke(tagRepository, context);
        return context;
    }

    private void invoke(TagRepository tagRepository, DefaultActivityStrategyFactory.DynamicContext context) throws Exception {
        TagNode tagNode = new TagNode();
        ReflectionTestUtils.setField(tagNode, "tagRepository", tagRepository);
        // 下一节点使用 mock，路由正常完成；判定逻辑本身抛出的任何异常都会使用例失败
        ReflectionTestUtils.setField(tagNode, "marketNode", mock(MarketNode.class));
        ReflectionTestUtils.setField(tagNode, "trialEndNode", mock(TrialEndNode.class));
        Method doApply = TagNode.class.getDeclaredMethod("doApply", MarketProductEntity.class, DefaultActivityStrategyFactory.DynamicContext.class);
        doApply.setAccessible(true);
        try {
            doApply.invoke(tagNode, MarketProductEntity.builder().userId("user01").goodsId("9890001").build(), context);
        } catch (InvocationTargetException e) {
            throw new AssertionError("人群标签节点不应抛出异常", e.getCause());
        }
    }

}
