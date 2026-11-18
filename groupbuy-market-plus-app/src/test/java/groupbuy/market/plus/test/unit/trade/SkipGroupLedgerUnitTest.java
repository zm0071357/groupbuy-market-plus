package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.valobj.SkipGroupBalanceVO;
import groupbuy.market.plus.infrastructure.adapter.repository.SkipGroupLedger;
import groupbuy.market.plus.infrastructure.dao.SkipGroupDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.SkipGroupRecord;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** 验证免拼余额口径及订单记录的状态转换。 */
public class SkipGroupLedgerUnitTest {
    private final SkipGroupDao dao = mock(SkipGroupDao.class);
    private final SkipGroupLedger ledger = new SkipGroupLedger();

    @Before
    public void init() {
        ReflectionTestUtils.setField(ledger, "dao", dao);
        when(dao.updateCounts(any())).thenReturn(1);
    }

    @Test
    public void calculatesProgressIncludingDebtAfterAnEarningRefund() {
        assertEquals(new SkipGroupBalanceVO(0, 2), SkipGroupBalanceVO.from(0, 0, 0));
        assertEquals(new SkipGroupBalanceVO(0, 1), SkipGroupBalanceVO.from(1, 0, 0));
        assertEquals(new SkipGroupBalanceVO(1, 2), SkipGroupBalanceVO.from(2, 0, 0));
        assertEquals(new SkipGroupBalanceVO(0, 2), SkipGroupBalanceVO.from(2, 1, 0));
        assertEquals(new SkipGroupBalanceVO(0, 3), SkipGroupBalanceVO.from(1, 0, 1));
        assertEquals(new SkipGroupBalanceVO(1, 2), SkipGroupBalanceVO.from(4, 0, 1));
    }

    @Test
    public void onlyCreatesHoldWhenAtomicReservationSucceeds() {
        assertEquals("E0045", assertThrows(AppException.class, () -> ledger.reserve("o-1", "u", "s", "c")).getCode());
        verify(dao, never()).insertRecord(any());

        when(dao.reserve(any())).thenReturn(1);
        ledger.reserve("o-2", "u", "s", "c");

        verify(dao).insertRecord(argThat(record -> record.getOrderId().equals("o-2") && record.getState().equals("HELD")));
    }

    @Test
    public void consumesOrReleasesTheExistingHold() {
        when(dao.getRecordForUpdate("used")).thenReturn(new SkipGroupRecord("used", "u", "s", "c", "HELD"));
        when(dao.getRecordForUpdate("canceled")).thenReturn(new SkipGroupRecord("canceled", "u", "s", "c", "HELD"));
        when(dao.updateRecord("used", "HELD", "USED")).thenReturn(1);
        when(dao.updateRecord("canceled", "HELD", "RELEASED")).thenReturn(1);

        ledger.consume("used");
        ledger.release("canceled");

        verify(dao).updateCounts(argThat(delta -> delta.getHeldCount() == -1 && delta.getUsedCount() == 1));
        verify(dao).updateCounts(argThat(delta -> delta.getHeldCount() == -1 && delta.getUsedCount() == 0));
    }

    @Test
    public void earnsPerOrderInAccountOrderAndRevokesOnlyEarnedRecords() {
        GroupBuyTeamOrder first = GroupBuyTeamOrder.builder().orderId("o1").userId("a").source("s").channel("c").build();
        GroupBuyTeamOrder second = GroupBuyTeamOrder.builder().orderId("o2").userId("z").source("s").channel("c").build();
        ledger.earn(List.of(second, first));
        ArgumentCaptor<SkipGroupRecord> records = ArgumentCaptor.forClass(SkipGroupRecord.class);
        verify(dao, times(2)).insertRecord(records.capture());
        assertEquals(List.of("o1", "o2"), records.getAllValues().stream().map(SkipGroupRecord::getOrderId).toList());

        when(dao.getRecordForUpdate("o1")).thenReturn(new SkipGroupRecord("o1", "a", "s", "c", "EARNED"));
        when(dao.getRecordForUpdate("skip")).thenReturn(new SkipGroupRecord("skip", "a", "s", "c", "USED"));
        when(dao.updateRecord("o1", "EARNED", "REVOKED")).thenReturn(1);

        ledger.revokeEarning("o1");
        ledger.revokeEarning("skip");
        ledger.revokeEarning("historical");

        verify(dao).updateCounts(argThat(delta -> delta.getEarnedCount() == -1 && delta.getUserId().equals("a")));
        verify(dao, times(1)).updateRecord(anyString(), anyString(), anyString());
    }
}
