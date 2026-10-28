package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.lock.filter.UserFilter;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.test.unit.mapper.MapperTestSupport;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 参与次数限制单元测试 - 达到上限即拒绝，退单订单不计入
 */
public class TakeLimitUnitTest {

    @Test
    public void test_reachLimit_reject() {
        UserFilter userFilter = buildFilter(1);
        try {
            userFilter.apply(buildCheckLock(), buildContext(1));
            Assert.fail("已参与 1 次、上限 1 次时应拒绝");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0008.getCode(), e.getCode());
        }
    }

    @Test
    public void test_belowLimit_pass() {
        Assert.assertNull(buildFilter(0).apply(buildCheckLock(), buildContext(1)));
    }

    @Test
    public void test_sql_excludeRefund() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_order_mapper.xml");
        String sql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao.checkUserTakeActivityCount", new GroupBuyTeamOrder());

        Assert.assertTrue(sql, sql.contains("and status in (0, 1)"));
    }

    private UserFilter buildFilter(int takeCount) {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.checkUserTakeActivityCount(100123L, "user01")).thenReturn(takeCount);
        UserFilter userFilter = new UserFilter();
        ReflectionTestUtils.setField(userFilter, "tradeRepository", tradeRepository);
        return userFilter;
    }

    private CheckLockEntity buildCheckLock() {
        return CheckLockEntity.builder().activityId(100123L).userId("user01").build();
    }

    private LockOrderLinkFactory.DynamicContext buildContext(int takeLimitCount) {
        ActivityEntity activityEntity = new ActivityEntity();
        activityEntity.setTakeLimitCount(takeLimitCount);
        LockOrderLinkFactory.DynamicContext context = new LockOrderLinkFactory.DynamicContext();
        context.setActivityEntity(activityEntity);
        return context;
    }

}
