package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 结算仓储单元测试 - 是否为最后一笔由数据库按最新完成量判断，不依赖事务外查询的完成量
 */
@RunWith(MockitoJUnitRunner.class)
public class SettleOrderRepositoryUnitTest {

    private static final String TEAM_ID = "TEAM12345678";

    @Mock
    private GroupBuyTeamDao groupBuyTeamDao;

    @Mock
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;

    @Mock
    private NotifyTaskDao notifyTaskDao;

    @InjectMocks
    private TradeRepositoryImpl tradeRepository;

    @Before
    public void setUp() {
        when(groupBuyTeamOrderDao.updateOrderStatusComplete(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamAddCompleteCount(TEAM_ID)).thenReturn(1);
    }

    /**
     * 并发结算：事务外读到的完成量还差2人（旧值），但数据库中本次结算后已满员 - 应判定拼团完成
     */
    @Test
    public void test_staleCompleteCount_dbCompleted_createNotifyTask() {
        when(groupBuyTeamDao.updateTeamStatusComplete(TEAM_ID)).thenReturn(1);
        GroupBuyTeamOrder order = new GroupBuyTeamOrder();
        order.setOutTradeNo("OUT0001");
        when(groupBuyTeamOrderDao.getCompleteTeamOutTradeNoList(TEAM_ID)).thenReturn(Collections.singletonList(order));

        NotifyTaskEntity notifyTaskEntity = tradeRepository.settleOrder(buildAggregate(3, 1));

        Assert.assertNotNull(notifyTaskEntity);
        Assert.assertEquals(TEAM_ID + "_1", notifyTaskEntity.getTaskId());
        verify(notifyTaskDao).insert(any(NotifyTask.class));
    }

    /**
     * 事务外读到的完成量显示为最后一笔（旧值），但数据库中尚未满员 - 不应判定拼团完成，也不应抛异常
     */
    @Test
    public void test_dbNotCompleted_noNotifyTask() {
        when(groupBuyTeamDao.updateTeamStatusComplete(TEAM_ID)).thenReturn(0);

        NotifyTaskEntity notifyTaskEntity = tradeRepository.settleOrder(buildAggregate(3, 2));

        Assert.assertNull(notifyTaskEntity);
        verify(notifyTaskDao, never()).insert(any(NotifyTask.class));
    }

    private SettleOrderAggregate buildAggregate(int targetCount, int staleCompleteCount) {
        return SettleOrderAggregate.builder()
                .userEntity(UserEntity.builder().userId("user01").build())
                .groupBuyTeamEntity(GroupBuyTeamEntity.builder()
                        .teamId(TEAM_ID)
                        .activityId(100123L)
                        .targetCount(targetCount)
                        .completeCount(staleCompleteCount)
                        .notifyConfigVO(NotifyConfigVO.builder()
                                .notifyTypeEnum(NotifyTypeEnum.HTTP)
                                .notifyUrl("http://127.0.0.1/notify")
                                .build())
                        .build())
                .orderPaySuccessEntity(OrderPaySuccessEntity.builder()
                        .userId("user01")
                        .outTradeNo("OUT0001")
                        .outTradeNoPayTime(new Date())
                        .build())
                .build();
    }

}
