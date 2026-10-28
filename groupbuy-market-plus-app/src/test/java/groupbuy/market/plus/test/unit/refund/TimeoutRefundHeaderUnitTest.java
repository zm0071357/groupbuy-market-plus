package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.node.RefundRepeatNode;
import groupbuy.market.plus.domain.trade.service.refund.node.RefundUserNode;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 超时退单不选新团长、订单不存在返回 E0013 单元测试
 */
public class TimeoutRefundHeaderUnitTest {

    @Test
    @SuppressWarnings("unchecked")
    public void test_timeoutRefund_markFlag() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        DefaultRefundStrategyFactory factory = mock(DefaultRefundStrategyFactory.class);
        StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> handler = mock(StrategyHandler.class);
        List<String> teamIdList = Collections.singletonList("TEAM12345678");
        when(tradeRepository.getTimeoutTeamIdList(null, GroupBuyConstants.TimeoutTeamPageSize)).thenReturn(teamIdList);
        when(tradeRepository.getTimeoutOrderList(teamIdList)).thenReturn(Collections.singletonList(PreRefundEntity.builder().userId("user01").outTradeNo("OUT0001").build()));
        when(factory.strategyHandler()).thenReturn(handler);
        when(handler.apply(any(), any())).thenReturn(RefundResEntity.builder().refundStatusEnum(RefundStatusEnum.SUCCESS).build());
        RefundOrderServiceImpl service = new RefundOrderServiceImpl();
        ReflectionTestUtils.setField(service, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(service, "defaultRefundStrategyFactory", factory);

        service.teamTimeoutRefund();

        ArgumentCaptor<PreRefundEntity> captor = ArgumentCaptor.forClass(PreRefundEntity.class);
        verify(handler).apply(captor.capture(), any());
        Assert.assertTrue(captor.getValue().isTimeoutRefund());
    }

    @Test
    public void test_timeoutRefund_headerNoNewLeader() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        RefundUserNode node = new RefundUserNode();
        ReflectionTestUtils.setField(node, "tradeRepository", tradeRepository);
        DefaultRefundStrategyFactory.DynamicContext context = new DefaultRefundStrategyFactory.DynamicContext();
        context.setPreRefundOrderEntity(PreRefundOrderEntity.builder().isHeader(true).orderStatusEnum(OrderStatusEnum.CREATE).build());
        context.setGroupBuyTeamEntity(GroupBuyTeamEntity.builder().teamId("TEAM12345678").teamStatusEnum(TeamStatusEnum.PROGRESS).build());

        invoke(node, RefundUserNode.class, PreRefundEntity.builder().userId("user01").timeoutRefund(true).build(), context);

        Assert.assertNull(context.getNewLeaderUserId());
        verify(tradeRepository, never()).queryNewHeaderUserId(anyString());
    }

    @Test
    public void test_orderNotExist_E0013() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.getRefundThreadTaskResAggregate(any(PreRefundEntity.class))).thenReturn(null);
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        // 同步执行提交的异步任务
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        RefundRepeatNode node = new RefundRepeatNode();
        ReflectionTestUtils.setField(node, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(node, "threadPoolExecutor", executor);

        Method multiThread = RefundRepeatNode.class.getDeclaredMethod("multiThread", PreRefundEntity.class, DefaultRefundStrategyFactory.DynamicContext.class);
        multiThread.setAccessible(true);
        try {
            multiThread.invoke(node, PreRefundEntity.builder().userId("user01").outTradeNo("OUT_NOT_EXIST").build(), new DefaultRefundStrategyFactory.DynamicContext());
            Assert.fail("应抛出 E0013");
        } catch (InvocationTargetException e) {
            Assert.assertTrue("应为 AppException，实际：" + e.getCause(), e.getCause() instanceof AppException);
            Assert.assertEquals(ResponseCodeEnum.E0013.getCode(), ((AppException) e.getCause()).getCode());
        }
    }

    private void invoke(Object node, Class<?> clazz, PreRefundEntity request, DefaultRefundStrategyFactory.DynamicContext context) throws Exception {
        Method doApply = clazz.getDeclaredMethod("doApply", PreRefundEntity.class, DefaultRefundStrategyFactory.DynamicContext.class);
        doApply.setAccessible(true);
        try {
            doApply.invoke(node, request, context);
        } catch (InvocationTargetException ignored) {
            // 路由到下一节点时未注入依赖，忽略
        }
    }

}
