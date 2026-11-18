package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.service.invite.InviteServiceImpl;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 组队库存恢复 Key 过期时间与邀请码失效判空单元测试
 */
public class TeamStockExpireUnitTest {

    private static final String RECOVER_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_recovery";

    @Test
    public void test_recoverOnLockFail_setExpire() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RAtomicLong recover = mock(RAtomicLong.class);
        when(redissonClient.getAtomicLong(RECOVER_KEY)).thenReturn(recover);
        when(recover.incrementAndGet()).thenReturn(1L);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);

        Assert.assertEquals(Long.valueOf(1), tradeRepository.recoverTeamStock(RECOVER_KEY, 15));
        verify(recover).expire(Duration.ofMinutes(75));
    }

    @Test
    public void test_inviteExpire_emptyList_skip() {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.getTimeoutTeamIdList()).thenReturn(Collections.emptyList());
        InviteServiceImpl inviteService = new InviteServiceImpl();
        ReflectionTestUtils.setField(inviteService, "tradeRepository", tradeRepository);

        inviteService.inviteExpire();

        verify(tradeRepository, never()).inviteExpire(anyList());
    }

}
