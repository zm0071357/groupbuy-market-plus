package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.service.task.TaskServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 回调任务服务单元测试 - 同一组队下的多个回调任务按任务ID各自更新状态
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceImplUnitTest {

    private static final String TEAM_ID = "TEAM12345678";

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradePort tradePort;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    public void test_sameTeamTasks_updateByTaskId() throws Exception {
        NotifyTaskEntity settleTask = buildTask(TEAM_ID + "_1", 0);
        NotifyTaskEntity refundTask = buildTask(TEAM_ID + "_2_ORDER000000000001", 0);
        when(tradeRepository.getUnNotifyOrderRefundTask()).thenReturn(Arrays.asList(settleTask, refundTask));
        when(tradePort.groupBuyNotify(settleTask)).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());
        when(tradePort.groupBuyNotify(refundTask)).thenReturn(NofifyStatusEnum.FAIL.getStatus());
        when(tradeRepository.updateNotifyTaskSuccess(settleTask.getTaskId())).thenReturn(1);
        when(tradeRepository.updateNotifyTaskRetry(refundTask.getTaskId())).thenReturn(1);

        Map<String, Integer> result = taskService.execOrderRefundNotifyJob();

        // 成功的任务只更新自己，失败的任务进入重试，互不影响
        verify(tradeRepository).updateNotifyTaskSuccess(settleTask.getTaskId());
        verify(tradeRepository).updateNotifyTaskRetry(refundTask.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(refundTask.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(TEAM_ID);
        verify(tradeRepository, never()).updateNotifyTaskFail(anyString());
        Assert.assertEquals(Integer.valueOf(1), result.get("successCount"));
        Assert.assertEquals(Integer.valueOf(1), result.get("retryCount"));
    }

    @Test
    public void test_failOverRetryLimit_updateFailByTaskId() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 5);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.FAIL.getStatus());
        when(tradeRepository.updateNotifyTaskFail(task.getTaskId())).thenReturn(1);

        Map<String, Integer> result = taskService.execNotifyJob(task);

        verify(tradeRepository).updateNotifyTaskFail(task.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        Assert.assertEquals(Integer.valueOf(1), result.get("failCount"));
    }

    @Test
    public void test_skippedTask_noStatusUpdate() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.NULL.getStatus());

        taskService.execNotifyJob(task);

        verify(tradeRepository, never()).updateNotifyTaskSuccess(anyString());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        verify(tradeRepository, never()).updateNotifyTaskFail(anyString());
    }

    @Test
    public void test_lockKey_byTaskId() {
        NotifyTaskEntity settleTask = buildTask(TEAM_ID + "_1", 0);
        NotifyTaskEntity refundTask = buildTask(TEAM_ID + "_2_ORDER000000000001", 0);
        Assert.assertNotEquals(settleTask.lockKey(), refundTask.lockKey());
        Assert.assertTrue(settleTask.lockKey().endsWith(settleTask.getTaskId()));
    }

    private NotifyTaskEntity buildTask(String taskId, int notifyCount) {
        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(TEAM_ID)
                .notifyTypeEnum(NotifyTypeEnum.HTTP)
                .notifyUrl("http://127.0.0.1/notify")
                .notifyCount(notifyCount)
                .parameterJson("{}")
                .build();
    }

}
