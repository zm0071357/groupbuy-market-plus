package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.node.TagNode;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.service.invite.factory.InviteLinkFactory;
import groupbuy.market.plus.domain.trade.service.invite.filter.TeamStatusFilter;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 空值保护单元测试 - 返回明确错误码而不是空指针
 */
public class NullGuardUnitTest {

    @Test
    public void test_trialWithoutActivity_E0002() throws Exception {
        TagNode tagNode = new TagNode();
        Method doApply = TagNode.class.getDeclaredMethod("doApply", MarketProductEntity.class, DefaultActivityStrategyFactory.DynamicContext.class);
        doApply.setAccessible(true);
        try {
            // 上下文中没有活动与商品（商品未配置拼团活动）
            doApply.invoke(tagNode, MarketProductEntity.builder().userId("user01").goodsId("9890001").build(), new DefaultActivityStrategyFactory.DynamicContext());
            Assert.fail("应抛出 E0002");
        } catch (InvocationTargetException e) {
            Assert.assertTrue("应为 AppException，实际：" + e.getCause(), e.getCause() instanceof AppException);
            Assert.assertEquals(ResponseCodeEnum.E0002.getCode(), ((AppException) e.getCause()).getCode());
        }
    }

    @Test
    public void test_inviteTeamNotExist_E0015() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.getTeamProgress("TEAM00000000")).thenReturn(null);
        TeamStatusFilter filter = new TeamStatusFilter();
        ReflectionTestUtils.setField(filter, "tradeRepository", tradeRepository);
        try {
            filter.apply(CheckInviteEntity.builder().inviteUserId("user01").teamId("TEAM00000000").build(), new InviteLinkFactory.DynamicContext());
            Assert.fail("应抛出 E0015");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0015.getCode(), e.getCode());
        }
    }

}
