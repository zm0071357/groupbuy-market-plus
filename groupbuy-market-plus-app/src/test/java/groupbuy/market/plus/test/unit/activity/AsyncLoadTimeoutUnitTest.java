package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.node.TagNode;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.node.RefundRepeatNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;

/**
 * 异步加载超时单元测试 - 线程池迟迟不执行任务时，应在约 3 秒内超时，而不是长时间阻塞
 */
public class AsyncLoadTimeoutUnitTest {

    @Test
    public void test_trialTagNode_timeoutInSeconds() throws Exception {
        TagNode tagNode = new TagNode();
        // 模拟线程池积压：提交的任务永远不会被执行
        ReflectionTestUtils.setField(tagNode, "threadPoolExecutor", mock(ThreadPoolExecutor.class));

        assertTimeout(tagNode, MarketProductEntity.builder().userId("user01").goodsId("9890001").source("s01").channel("c01").build(),
                new DefaultActivityStrategyFactory.DynamicContext());
    }

    @Test
    public void test_refundRepeatNode_timeoutInSeconds() throws Exception {
        RefundRepeatNode refundRepeatNode = new RefundRepeatNode();
        ReflectionTestUtils.setField(refundRepeatNode, "threadPoolExecutor", mock(ThreadPoolExecutor.class));

        assertTimeout(refundRepeatNode, PreRefundEntity.builder().userId("user01").outTradeNo("OUT0001").build(),
                new DefaultRefundStrategyFactory.DynamicContext());
    }

    private void assertTimeout(Object node, Object requestParameter, Object dynamicContext) throws Exception {
        Method multiThread = findMultiThread(node.getClass(), requestParameter.getClass(), dynamicContext.getClass());
        multiThread.setAccessible(true);
        long start = System.currentTimeMillis();
        try {
            multiThread.invoke(node, requestParameter, dynamicContext);
            Assert.fail("应抛出 TimeoutException");
        } catch (InvocationTargetException e) {
            Assert.assertTrue("应抛出 TimeoutException，实际：" + e.getCause(), e.getCause() instanceof TimeoutException);
        }
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue("超时过早：" + elapsed + "ms", elapsed >= 2900);
        Assert.assertTrue("超时过晚：" + elapsed + "ms", elapsed < 10000);
    }

    private Method findMultiThread(Class<?> clazz, Class<?> requestType, Class<?> contextType) throws NoSuchMethodException {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod("multiThread", requestType, contextType);
            } catch (NoSuchMethodException ignored) {
                // 继续向父类查找
            }
        }
        throw new NoSuchMethodException("multiThread");
    }

}
