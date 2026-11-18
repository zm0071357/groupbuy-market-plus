package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LockOrderAmountUnitTest {

    @Test
    public void persistsAndReturnsTotalDiscountIncludingHeaderDiscount() {
        TradeRepositoryImpl repository = new TradeRepositoryImpl();
        GroupBuyTeamOrderDao orderDao = mock(GroupBuyTeamOrderDao.class);
        ReflectionTestUtils.setField(repository, "groupBuyTeamOrderDao", orderDao);
        ReflectionTestUtils.setField(repository, "groupBuyTeamDao", mock(GroupBuyTeamDao.class));
        LockOrderEntity result = repository.lockOrder(LockOrderAggregate.builder()
                .userEntity(UserEntity.builder().userId("user01").build())
                .groupBuyTeamEntity(GroupBuyTeamEntity.builder().activityId(100123L).validTime(15)
                        .notifyConfigVO(NotifyConfigVO.builder().notifyTypeEnum(NotifyTypeEnum.MQ).build()).build())
                .orderDetailEntity(OrderDetailEntity.builder().originalPrice(new BigDecimal("100.00"))
                        .deductionPrice(new BigDecimal("20.00")).payPrice(new BigDecimal("80.00")).build())
                .checkLockResEntity(CheckLockResEntity.builder().isHeader(true).build())
                .build());

        ArgumentCaptor<GroupBuyTeamOrder> saved = ArgumentCaptor.forClass(GroupBuyTeamOrder.class);
        verify(orderDao).insert(saved.capture());
        assertEquals(new BigDecimal("76.00"), result.getPayPrice());
        assertEquals(new BigDecimal("24.00"), result.getDeductionPrice());
        assertEquals(result.getDeductionPrice(), saved.getValue().getDeductionPrice());
    }

    @Test
    public void replayUsesActualAmountsForLegacyOrder() {
        TradeRepositoryImpl repository = new TradeRepositoryImpl();
        GroupBuyTeamOrderDao orderDao = mock(GroupBuyTeamOrderDao.class);
        ReflectionTestUtils.setField(repository, "groupBuyTeamOrderDao", orderDao);
        when(orderDao.getNoPayLockOrderByOutTradeNo(any())).thenReturn(GroupBuyTeamOrder.builder()
                .originalPrice(new BigDecimal("100.00")).payPrice(new BigDecimal("76.00"))
                .deductionPrice(new BigDecimal("20.00")).status(0).build());

        assertEquals(new BigDecimal("24.00"), repository.getNoPayLockOrderByOutTradeNo("user01", "out1").getDeductionPrice());
    }
}
