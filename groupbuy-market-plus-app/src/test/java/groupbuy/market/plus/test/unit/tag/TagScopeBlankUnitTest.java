package groupbuy.market.plus.test.unit.tag;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import org.junit.Assert;
import org.junit.Test;

/**
 * 人群范围判空单元测试 - 配置了人群标签但未设置人群范围时，按「可见、可参与」处理，不抛空指针
 */
public class TagScopeBlankUnitTest {

    @Test
    public void test_blankTagScope_visibleAndEnable() {
        for (String blank : new String[]{null, "", " "}) {
            ActivityVO activityVO = ActivityVO.builder().tagId("RQ_KJHKL98UU78H66554GFDV").tagScope(blank).build();
            Assert.assertTrue("人群范围 [" + blank + "] 应可见", activityVO.isVisible());
            Assert.assertTrue("人群范围 [" + blank + "] 应可参与", activityVO.isEnable());
        }
    }

    @Test
    public void test_tagScope_keepOriginRule() {
        Assert.assertFalse(ActivityVO.builder().tagScope("1,1").build().isVisible());
        Assert.assertFalse(ActivityVO.builder().tagScope("1,1").build().isEnable());
        Assert.assertTrue(ActivityVO.builder().tagScope("2,2").build().isVisible());
        Assert.assertTrue(ActivityVO.builder().tagScope("2,2").build().isEnable());
    }

}
