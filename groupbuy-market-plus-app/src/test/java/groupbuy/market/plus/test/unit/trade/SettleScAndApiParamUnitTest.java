package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.dto.TeamProgressResponseDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderService;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.settle.filter.OrderStatusFilter;
import groupbuy.market.plus.domain.trade.service.settle.filter.OrderTimeFilter;
import groupbuy.market.plus.domain.trade.service.settle.filter.SCFilter;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.trigger.http.TradeController;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 结算来源渠道校验、锁单回调参数校验、组队进度判空、团长退单回调可选 单元测试
 */
public class SettleScAndApiParamUnitTest {

    @Test
    public void test_settle_scMismatch_E0032() {
        assertSettle(buildSettleChain(false), "s02", "c02", ResponseCodeEnum.E0032);
    }

    @Test
    public void test_settle_orderScBlack_E0017() {
        // 请求与订单来源渠道一致，订单来源渠道在黑名单中
        assertSettle(buildSettleChain(true), "s01", "c01", ResponseCodeEnum.E0017);
    }

    @Test
    public void test_lockOrder_notifyParamInvalid() {
        TradeController controller = new TradeController();
        LockOrderService lockOrderService = mock(LockOrderService.class);
        ReflectionTestUtils.setField(controller, "lockOrderService", lockOrderService);

        // 回调类型非法
        LockOrderRequestDTO request = buildLockRequest();
        request.getNotifyConfig().setNotifyType(3);
        Assert.assertEquals(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), controller.lockOrder(request).getCode());
        // 回调类型为空
        request.getNotifyConfig().setNotifyType(null);
        Assert.assertEquals(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), controller.lockOrder(request).getCode());
        // HTTP 缺少成团回调地址
        request.getNotifyConfig().setNotifyType(1);
        request.getNotifyConfig().setNotifyUrl(" ");
        Response<LockOrderResponseDTO> response = controller.lockOrder(request);
        Assert.assertEquals(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), response.getCode());
        verifyNoInteractions(lockOrderService);
    }

    @Test
    public void test_teamProgress_notExist_E0015() {
        TradeController controller = new TradeController();
        RefundOrderService refundOrderService = mock(RefundOrderService.class);
        when(refundOrderService.getTeamProgress("TEAM00000000")).thenReturn(null);
        ReflectionTestUtils.setField(controller, "refundOrderService", refundOrderService);

        Response<TeamProgressResponseDTO> response = controller.getTeamProgress("TEAM00000000");

        Assert.assertEquals(ResponseCodeEnum.E0015.getCode(), response.getCode());
    }

    @Test
    public void test_newHeaderNotify_httpWithoutUrl_noTask() {
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        GroupBuyTeamEntity team = GroupBuyTeamEntity.builder()
                .teamId("TEAM12345678")
                .notifyConfigVO(NotifyConfigVO.builder().notifyTypeEnum(NotifyTypeEnum.HTTP).headerRefundNotifyUrl(null).build())
                .build();

        Assert.assertNull(tradeRepository.newHeaderNotify("user02", "ORDER000000000001", team));
    }

    private BusinessLinkedList<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> buildSettleChain(boolean orderScBlack) {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.checkLockOrderStatusByOutTradeNo("user01", "OUT0001")).thenReturn(LockOrderEntity.builder()
                .orderId("ORDER000000000001").teamId("TEAM12345678").source("s01").channel("c01")
                .orderStatusEnum(OrderStatusEnum.CREATE).build());
        when(tradeRepository.isBlack("s01", "c01")).thenReturn(orderScBlack);
        SCFilter scFilter = new SCFilter();
        OrderStatusFilter orderStatusFilter = new OrderStatusFilter();
        OrderTimeFilter orderTimeFilter = new OrderTimeFilter();
        for (Object filter : new Object[]{scFilter, orderStatusFilter, orderTimeFilter}) {
            ReflectionTestUtils.setField(filter, "tradeRepository", tradeRepository);
        }
        return new SettleOrderLinkFactory().settleOrderLink(scFilter, orderStatusFilter, orderTimeFilter);
    }

    private void assertSettle(BusinessLinkedList<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> chain,
                              String source, String channel, ResponseCodeEnum expected) {
        try {
            chain.apply(CheckSettleEntity.builder().userId("user01").outTradeNo("OUT0001").source(source).channel(channel)
                    .outTradeNoPayTime(new Date()).build(), new SettleOrderLinkFactory.DynamicContext());
            Assert.fail("应抛出 " + expected.getCode());
        } catch (AppException e) {
            Assert.assertEquals(expected.getCode(), e.getCode());
        } catch (Exception e) {
            Assert.fail("应抛出 AppException，实际：" + e);
        }
    }

    private LockOrderRequestDTO buildLockRequest() {
        LockOrderRequestDTO request = new LockOrderRequestDTO();
        request.setUserId("user01");
        request.setActivityId(100123L);
        request.setGoodsId("9890001");
        request.setSource("s01");
        request.setChannel("c01");
        request.setOutTradeNo("OUT0001");
        request.setHTTPNotify("http://127.0.0.1/notify", null);
        return request;
    }

}
