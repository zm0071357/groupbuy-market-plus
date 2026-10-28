package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.service.invite.InviteServiceImpl;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 组队库存 Key 过期时间与邀请码失效判空单元测试
 */
public class TeamStockExpireUnitTest {

    private static final String OCCUPY_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_occupy";
    private static final String RECOVER_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_recovery";

    @Test
    @SuppressWarnings("unchecked")
    public void test_occupy_setExpire() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RAtomicLong occupy = mock(RAtomicLong.class);
        RAtomicLong recover = mock(RAtomicLong.class);
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getAtomicLong(OCCUPY_KEY)).thenReturn(occupy);
        when(redissonClient.getAtomicLong(RECOVER_KEY)).thenReturn(recover);
        when(redissonClient.getBucket(anyString())).thenReturn(bucket);
        when(bucket.trySet(any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(recover.get()).thenReturn(0L);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);

        // 抢占成功：两个 Key 均设置 15 + 60 分钟过期
        when(occupy.incrementAndGet()).thenReturn(1L);
        Assert.assertTrue(tradeRepository.occupyTeamStock(3, 15, OCCUPY_KEY, RECOVER_KEY));
        verify(occupy).expire(Duration.ofMinutes(75));
        verify(recover).expire(Duration.ofMinutes(75));

        // 抢占失败：重置抢占量后重新设置过期时间（SET 会清除过期时间）
        reset(occupy);
        when(occupy.incrementAndGet()).thenReturn(5L);
        Assert.assertFalse(tradeRepository.occupyTeamStock(3, 15, OCCUPY_KEY, RECOVER_KEY));
        verify(occupy).set(3);
        verify(occupy, times(2)).expire(Duration.ofMinutes(75));
    }

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
        when(tradeRepository.getTimeoutTeamIdList(null, GroupBuyConstants.TimeoutTeamPageSize)).thenReturn(Collections.emptyList());
        InviteServiceImpl inviteService = new InviteServiceImpl();
        ReflectionTestUtils.setField(inviteService, "tradeRepository", tradeRepository);

        inviteService.inviteExpire();

        verify(tradeRepository, never()).inviteExpire(anyList());
    }

}
