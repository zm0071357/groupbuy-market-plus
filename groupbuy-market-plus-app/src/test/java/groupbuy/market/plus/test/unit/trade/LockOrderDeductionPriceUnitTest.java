package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 锁单优惠金额单元测试 - 团长额外优惠计入优惠金额，保证 原价 = 优惠金额 + 支付价格
 */
@RunWith(MockitoJUnitRunner.class)
public class LockOrderDeductionPriceUnitTest {

    @Mock
    private GroupBuyTeamDao groupBuyTeamDao;

    @Mock
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;

    @InjectMocks
    private TradeRepositoryImpl tradeRepository;

    @Test
    public void test_header_deductionIncludeHeaderDiscount() {
        // 原价 100，试算优惠 20，试算价 80；团长再打 95 折 -> 支付 76.00，优惠应为 24.00
        LockOrderEntity lockOrderEntity = tradeRepository.lockOrder(buildAggregate(true, null));

        GroupBuyTeamOrder order = captureOrder();
        Assert.assertEquals(new BigDecimal("76.00"), order.getPayPrice());
        Assert.assertEquals(0, new BigDecimal("24.00").compareTo(order.getDeductionPrice()));
        Assert.assertEquals(0, order.getOriginalPrice().compareTo(order.getDeductionPrice().add(order.getPayPrice())));
        // 返回给商城的锁单结果与落库一致
        Assert.assertEquals(0, new BigDecimal("24.00").compareTo(lockOrderEntity.getDeductionPrice()));
        Assert.assertEquals(new BigDecimal("76.00"), lockOrderEntity.getPayPrice());
    }

    @Test
    public void test_member_keepTrialDeduction() {
        when(groupBuyTeamDao.updateLockCount(anyString())).thenReturn(1);

        LockOrderEntity lockOrderEntity = tradeRepository.lockOrder(buildAggregate(false, "TEAM12345678"));

        GroupBuyTeamOrder order = captureOrder();
        Assert.assertEquals(0, new BigDecimal("20").compareTo(order.getDeductionPrice()));
        Assert.assertEquals(0, new BigDecimal("80").compareTo(order.getPayPrice()));
        Assert.assertEquals(0, new BigDecimal("20").compareTo(lockOrderEntity.getDeductionPrice()));
    }

    private GroupBuyTeamOrder captureOrder() {
        ArgumentCaptor<GroupBuyTeamOrder> captor = ArgumentCaptor.forClass(GroupBuyTeamOrder.class);
        verify(groupBuyTeamOrderDao).insert(captor.capture());
        return captor.getValue();
    }

    private LockOrderAggregate buildAggregate(boolean isHeader, String teamId) {
        return LockOrderAggregate.builder()
                .userEntity(UserEntity.builder().userId("user01").build())
                .groupBuyTeamEntity(GroupBuyTeamEntity.builder()
                        .teamId(teamId)
                        .activityId(100123L)
                        .goodsId("9890001")
                        .targetCount(3)
                        .validTime(15)
                        .notifyConfigVO(NotifyConfigVO.builder().notifyTypeEnum(NotifyTypeEnum.MQ).build())
                        .build())
                .orderDetailEntity(OrderDetailEntity.builder()
                        .outTradeNo("OUT0001")
                        .goodsId("9890001")
                        .source("s01")
                        .channel("c01")
                        .originalPrice(new BigDecimal("100"))
                        .deductionPrice(new BigDecimal("20"))
                        .payPrice(new BigDecimal("80"))
                        .build())
                .checkLockResEntity(CheckLockResEntity.builder().isHeader(isHeader).build())
                .build();
    }

}
