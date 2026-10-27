package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.port.TradePortImpl;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.event.EventPublisher;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 回调端口单元测试 - 防重复回调
 */
@RunWith(MockitoJUnitRunner.class)
public class TradePortImplUnitTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private NotifyTaskDao notifyTaskDao;

    @Mock
    private NotifyTaskServiceImpl notifyTaskServiceImpl;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private TradePortImpl tradePort;

    private final NotifyTaskEntity task = NotifyTaskEntity.builder()
            .taskId("TEAM12345678_1")
            .teamId("TEAM12345678")
            .notifyTypeEnum(NotifyTypeEnum.HTTP)
            .notifyUrl("http://127.0.0.1/notify")
            .notifyCount(0)
            .parameterJson("{}")
            .build();

    @Before
    public void setUp() throws Exception {
        when(redissonClient.getLock(task.lockKey())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    public void test_alreadySuccess_skipNotify() throws Exception {
        when(notifyTaskDao.getNotifyStatusByTaskId(task.getTaskId())).thenReturn(1);

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.NULL.getStatus(), status);
        verify(notifyTaskServiceImpl, never()).notify(anyString(), anyString());
        verify(lock).unlock();
    }

    @Test
    public void test_notSuccess_doNotify() throws Exception {
        when(notifyTaskDao.getNotifyStatusByTaskId(task.getTaskId())).thenReturn(2);
        when(notifyTaskServiceImpl.notify(task.getNotifyUrl(), task.getParameterJson())).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.SUCCESS.getStatus(), status);
        verify(notifyTaskServiceImpl).notify(task.getNotifyUrl(), task.getParameterJson());
    }

}
