package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.service.invite.InviteServiceImpl;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Arrays;
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
    public void test_occupy_success_lockBySequence() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        RBucket<Object> bucket = mock(RBucket.class);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(redissonClient.getBucket(anyString())).thenReturn(bucket);
        when(bucket.trySet(any(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);

        // 脚本返回本次抢占后的总占位（含团长）
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any(), any())).thenReturn(2L);
        Assert.assertTrue(tradeRepository.occupyTeamStock(3, 15, OCCUPY_KEY, RECOVER_KEY));

        // 抢占Key在前、恢复Key在后；目标量 3；两个 Key 过期时间 = (15 + 60) 分钟
        ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
        verify(script).eval(eq(RScript.Mode.READ_WRITE), scriptCaptor.capture(), eq(RScript.ReturnType.INTEGER),
                eq(Arrays.asList(OCCUPY_KEY, RECOVER_KEY)), eq("3"), eq(String.valueOf(Duration.ofMinutes(75).toMillis())));
        // 位置不足时在脚本内回滚，不再重置抢占量为目标量
        Assert.assertTrue(scriptCaptor.getValue().contains("redis.call('decr', KEYS[1])"));
        Assert.assertFalse(scriptCaptor.getValue().contains("'set'"));
        // 按返回的总占位加锁兜底
        verify(redissonClient).getBucket(OCCUPY_KEY + "_2");
        // 不再通过 RAtomicLong 分步读写抢占量
        verify(redissonClient, never()).getAtomicLong(anyString());
    }

    @Test
    public void test_occupy_full_returnFalse() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);

        // 脚本返回 -1：位置不足（脚本内已回滚本次抢占）
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any(), any())).thenReturn(-1L);
        Assert.assertFalse(tradeRepository.occupyTeamStock(3, 15, OCCUPY_KEY, RECOVER_KEY));

        // 失败时不加兜底锁、不修改抢占量
        verify(redissonClient, never()).getBucket(anyString());
        verify(redissonClient, never()).getAtomicLong(anyString());
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
        when(tradeRepository.getTimeoutTeamIdList()).thenReturn(Collections.emptyList());
        InviteServiceImpl inviteService = new InviteServiceImpl();
        ReflectionTestUtils.setField(inviteService, "tradeRepository", tradeRepository);

        inviteService.inviteExpire();

        verify(tradeRepository, never()).inviteExpire(anyList());
    }

}
