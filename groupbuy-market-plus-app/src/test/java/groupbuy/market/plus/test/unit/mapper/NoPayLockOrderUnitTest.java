package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁单幂等查询单元测试 - 查询团长标识且空值安全
 */
public class NoPayLockOrderUnitTest {

    @Test
    public void test_sql_selectIsHeader() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_order_mapper.xml");
        String sql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao.getNoPayLockOrderByOutTradeNo", new GroupBuyTeamOrder());

        Assert.assertTrue(sql, sql.contains("is_header"));
    }

    @Test
    public void test_isHeader_nullSafe() {
        GroupBuyTeamOrderDao groupBuyTeamOrderDao = mock(GroupBuyTeamOrderDao.class);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "groupBuyTeamOrderDao", groupBuyTeamOrderDao);

        GroupBuyTeamOrder order = new GroupBuyTeamOrder();
        order.setOrderId("ORDER000000000001");
        order.setStatus(0);
        when(groupBuyTeamOrderDao.getNoPayLockOrderByOutTradeNo(any(GroupBuyTeamOrder.class))).thenReturn(order);
        // is_header 为空时不抛空指针，按非团长处理
        LockOrderEntity lockOrderEntity = tradeRepository.getNoPayLockOrderByOutTradeNo("user01", "OUT0001");
        Assert.assertFalse(lockOrderEntity.getIsHeader());

        order.setIsHeader(1);
        Assert.assertTrue(tradeRepository.getNoPayLockOrderByOutTradeNo("user01", "OUT0001").getIsHeader());
    }

}
