package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderDetailEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderService;
import groupbuy.market.plus.trigger.http.TradeController;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 锁单活动校验单元测试 - 请求活动必须与商品匹配的活动一致
 */
@RunWith(MockitoJUnitRunner.class)
public class LockOrderActivityCheckUnitTest {

    private static final Long ACTIVITY_ID = 100123L;

    @Mock
    private LockOrderService lockOrderService;

    @Mock
    private IndexGroupBuyMarketService indexGroupBuyMarketService;

    @InjectMocks
    private TradeController tradeController;

    private LockOrderRequestDTO request;

    @Before
    public void setUp() throws Exception {
        request = new LockOrderRequestDTO();
        request.setUserId("user01");
        request.setActivityId(ACTIVITY_ID);
        request.setGoodsId("9890001");
        request.setSource("s01");
        request.setChannel("c01");
        request.setOutTradeNo("OUT0001");
        request.setHTTPNotify("http://127.0.0.1/notify", "http://127.0.0.1/header_refund_notify");
        when(lockOrderService.getNoPayLockOrderByOutTradeNo("user01", "OUT0001")).thenReturn(null);
    }

    @Test
    public void test_activityMismatch_reject() throws Exception {
        when(indexGroupBuyMarketService.indexMarketTrial(any(MarketProductEntity.class))).thenReturn(buildTrial(999999L));

        Response<LockOrderResponseDTO> response = tradeController.lockOrder(request);

        Assert.assertEquals(ResponseCodeEnum.E0027.getCode(), response.getCode());
        verify(lockOrderService, never()).lockOrder(any(), any(), any());
    }

    @Test
    public void test_activityMatch_lockOrder() throws Exception {
        when(indexGroupBuyMarketService.indexMarketTrial(any(MarketProductEntity.class))).thenReturn(buildTrial(ACTIVITY_ID));
        when(lockOrderService.lockOrder(any(UserEntity.class), any(GroupBuyTeamEntity.class), any(OrderDetailEntity.class)))
                .thenReturn(LockOrderEntity.builder()
                        .orderId("ORDER000000000001")
                        .isHeader(true)
                        .payPrice(new BigDecimal("80.00"))
                        .orderStatusEnum(OrderStatusEnum.CREATE)
                        .build());

        Response<LockOrderResponseDTO> response = tradeController.lockOrder(request);

        Assert.assertEquals(ResponseCodeEnum.SUCCESS.getCode(), response.getCode());
        verify(lockOrderService).lockOrder(any(UserEntity.class), any(GroupBuyTeamEntity.class), any(OrderDetailEntity.class));
    }

    private TrialBalanceEntity buildTrial(Long activityId) {
        return TrialBalanceEntity.builder()
                .goodsId("9890001")
                .originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("20.00"))
                .payPrice(new BigDecimal("80.00"))
                .activityVO(ActivityVO.builder()
                        .activityId(activityId)
                        .target(3)
                        .validTime(15)
                        .startTime(new Date())
                        .endTime(new Date())
                        .build())
                .build();
    }

}
