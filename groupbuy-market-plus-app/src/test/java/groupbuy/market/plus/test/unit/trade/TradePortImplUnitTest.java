package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.port.TradePortImpl;
import groupbuy.market.plus.infrastructure.event.EventPublisher;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 回调端口单元测试 - 只负责按回调类型发送通知，防重复回调由 TaskServiceImpl 加锁完成
 */
@RunWith(MockitoJUnitRunner.class)
public class TradePortImplUnitTest {

    @Mock
    private NotifyTaskServiceImpl notifyTaskServiceImpl;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private TradePortImpl tradePort;

    private final NotifyTaskEntity httpTask = NotifyTaskEntity.builder()
            .taskId("TEAM12345678_1")
            .teamId("TEAM12345678")
            .notifyTypeEnum(NotifyTypeEnum.HTTP)
            .notifyUrl("http://127.0.0.1/notify")
            .notifyCount(0)
            .parameterJson("{}")
            .build();

    @Test
    public void test_http_returnNotifyResult() throws Exception {
        when(notifyTaskServiceImpl.notify(httpTask.getNotifyUrl(), httpTask.getParameterJson())).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());

        Assert.assertEquals(NofifyStatusEnum.SUCCESS.getStatus(), tradePort.groupBuyNotify(httpTask));
        verify(notifyTaskServiceImpl).notify(httpTask.getNotifyUrl(), httpTask.getParameterJson());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    public void test_mq_publishAndSuccess() throws Exception {
        NotifyTaskEntity mqTask = NotifyTaskEntity.builder()
                .taskId("TEAM12345678_2_ORDER000000000001")
                .teamId("TEAM12345678")
                .notifyTypeEnum(NotifyTypeEnum.MQ)
                .notifyMQ("topic.order_refund")
                .notifyCount(0)
                .parameterJson("{}")
                .build();

        Assert.assertEquals(NofifyStatusEnum.SUCCESS.getStatus(), tradePort.groupBuyNotify(mqTask));
        verify(eventPublisher).publish("topic.order_refund", "{}");
        verify(notifyTaskServiceImpl, never()).notify(anyString(), anyString());
    }

    /**
     * 回调本身抛异常时返回失败，且不能给当前线程设置中断标记
     */
    @Test
    public void test_notifyException_notInterruptThread() throws Exception {
        when(notifyTaskServiceImpl.notify(httpTask.getNotifyUrl(), httpTask.getParameterJson())).thenThrow(new RuntimeException("connect timed out"));

        String status = tradePort.groupBuyNotify(httpTask);

        Assert.assertEquals(NofifyStatusEnum.FAIL.getStatus(), status);
        Assert.assertFalse("回调异常不应设置线程中断标记", Thread.interrupted());
    }

}
