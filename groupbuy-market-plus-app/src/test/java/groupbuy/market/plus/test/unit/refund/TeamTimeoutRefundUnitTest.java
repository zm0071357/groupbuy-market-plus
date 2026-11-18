package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 拼团超时退单单元测试 - 单条退单失败不影响后续订单
 */
@RunWith(MockitoJUnitRunner.class)
public class TeamTimeoutRefundUnitTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private DefaultRefundStrategyFactory defaultRefundStrategyFactory;

    @Mock
    private StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> strategyHandler;

    @InjectMocks
    private RefundOrderServiceImpl refundOrderService;

    @Test
    public void test_batchCountsSkippedAndContinuesAfterFailure() throws Exception {
        PreRefundEntity order1 = buildOrder("OUT0001");
        PreRefundEntity order2 = buildOrder("OUT0002");
        PreRefundEntity order3 = buildOrder("OUT0003");
        PreRefundEntity order4 = buildOrder("OUT0004");
        List<String> teamIdList = Collections.singletonList("TEAM12345678");
        when(tradeRepository.getTimeoutTeamIdList()).thenReturn(teamIdList);
        when(tradeRepository.getTimeoutOrderList(teamIdList)).thenReturn(Arrays.asList(order1, order2, order3, order4));
        when(defaultRefundStrategyFactory.strategyHandler()).thenReturn(strategyHandler);
        RefundResEntity success = RefundResEntity.builder().refundStatusEnum(RefundStatusEnum.SUCCESS).build();
        when(strategyHandler.apply(eq(order1), any())).thenReturn(success);
        when(strategyHandler.apply(eq(order2), any())).thenThrow(new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo()));
        when(strategyHandler.apply(eq(order3), any())).thenReturn(
                RefundResEntity.builder().refundStatusEnum(RefundStatusEnum.SKIPPED).build());
        when(strategyHandler.apply(eq(order4), any())).thenReturn(success);

        Map<String, Integer> result = refundOrderService.teamTimeoutRefund();

        // 失败和跳过均不影响后续订单处理。
        verify(strategyHandler).apply(eq(order4), any());
        Assert.assertEquals(Integer.valueOf(4), result.get("size"));
        Assert.assertEquals(Integer.valueOf(2), result.get("successCount"));
        Assert.assertEquals(Integer.valueOf(1), result.get("failCount"));
        Assert.assertEquals(Integer.valueOf(1), result.get("skippedCount"));
    }

    private PreRefundEntity buildOrder(String outTradeNo) {
        return PreRefundEntity.builder()
                .userId("user01")
                .outTradeNo(outTradeNo)
                .source("s01")
                .channel("c01")
                .build();
    }

}
