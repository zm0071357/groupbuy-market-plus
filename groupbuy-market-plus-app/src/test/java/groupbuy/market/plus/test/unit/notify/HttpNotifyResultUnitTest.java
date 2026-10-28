package groupbuy.market.plus.test.unit.notify;

import com.sun.net.httpserver.HttpServer;
import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.service.task.TaskServiceImpl;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * HTTP 回调结果判定单元测试 - 使用 JDK 内置 HttpServer 模拟商户回调接口
 */
public class HttpNotifyResultUnitTest {

    private HttpServer server;
    private String baseUrl;
    private NotifyTaskServiceImpl notifyTaskService;

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        respond("/success", 200, "SUCCESS");
        respond("/lowercase", 200, " success\n");
        respond("/error500", 500, "SUCCESS");
        respond("/json", 200, "{\"code\":\"0000\"}");
        respond("/empty", 200, "");
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        notifyTaskService = new NotifyTaskServiceImpl();
        ReflectionTestUtils.setField(notifyTaskService, "okHttpClient", new OkHttpClient());
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void test_httpResult() {
        Assert.assertEquals("SUCCESS", notify("/success"));
        Assert.assertEquals("忽略大小写与首尾空白", "SUCCESS", notify("/lowercase"));
        Assert.assertEquals("非 2xx 状态码按失败", "FAIL", notify("/error500"));
        Assert.assertEquals("非成功标识按失败", "FAIL", notify("/json"));
        Assert.assertEquals("空响应按失败", "FAIL", notify("/empty"));
    }

    @Test
    public void test_unknownStatus_countAsFail() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        TradePort tradePort = mock(TradePort.class);
        TaskServiceImpl taskService = new TaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(taskService, "tradePort", tradePort);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        ReflectionTestUtils.setField(taskService, "redissonClient", redissonClient);
        NotifyTaskEntity task = NotifyTaskEntity.builder().taskId("TEAM12345678_1").teamId("TEAM12345678")
                .notifyTypeEnum(NotifyTypeEnum.HTTP).notifyCount(0).build();
        when(tradeRepository.getNotifyStatus("TEAM12345678_1")).thenReturn(0);
        // 端口层返回约定之外的结果时，按失败累加重试次数，而不是什么都不做
        when(tradePort.groupBuyNotify(task)).thenReturn("UNKNOWN");

        taskService.execNotifyJob(task);

        verify(tradeRepository).updateNotifyTaskRetry("TEAM12345678_1");
        // 空执行（未抢到锁 / 已回调成功）不更新状态
        reset(tradeRepository);
        when(tradeRepository.getNotifyStatus("TEAM12345678_1")).thenReturn(0);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.NULL.getStatus());
        taskService.execNotifyJob(task);
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
    }

    private String notify(String path) {
        return notifyTaskService.notify(baseUrl + path, "{}");
    }

    private void respond(String path, int code, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
    }

}
