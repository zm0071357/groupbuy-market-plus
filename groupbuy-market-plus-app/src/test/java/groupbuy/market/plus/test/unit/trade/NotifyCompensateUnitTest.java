package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.service.task.TaskServiceImpl;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 回调补偿路径单元测试 - 从数据库读出的补偿任务必须携带任务ID
 */
public class NotifyCompensateUnitTest {

    @Test
    public void test_compensateTask_carryTaskId() {
        NotifyTaskDao notifyTaskDao = mock(NotifyTaskDao.class);
        NotifyTask notifyTask = NotifyTask.builder()
                .taskId("TEAM12345678_1")
                .teamId("TEAM12345678")
                .notifyType(NotifyTypeEnum.HTTP.getType())
                .notifyUrl("http://127.0.0.1/notify")
                .notifyCount(1)
                .notifyStatus(2)
                .parameterJson("{}")
                .build();
        when(notifyTaskDao.getUnNotifyTeamSuccessTaskList()).thenReturn(Collections.singletonList(notifyTask));
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "notifyTaskDao", notifyTaskDao);

        List<NotifyTaskEntity> taskList = tradeRepository.getUnNotifyTeamSuccessTask();

        Assert.assertEquals("TEAM12345678_1", taskList.get(0).getTaskId());
        Assert.assertEquals("notify_job_lock_key_TEAM12345678_1", taskList.get(0).lockKey());
    }

    @Test
    public void test_blankTaskId_skip() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        TradePort tradePort = mock(TradePort.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(taskService, "tradePort", tradePort);
        when(tradeRepository.getUnNotifyTeamSuccessTask()).thenReturn(Collections.singletonList(
                NotifyTaskEntity.builder().teamId("TEAM12345678").notifyTypeEnum(NotifyTypeEnum.HTTP).notifyCount(0).build()));

        taskService.execTeamSuccessNotifyJob();

        // 缺少任务ID时不回调、不更新状态
        verify(tradePort, never()).groupBuyNotify(any());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(anyString());
        verify(tradeRepository, never()).updateNotifyTaskRetry(any());
        verify(tradeRepository, never()).updateNotifyTaskFail(any());
    }

}
