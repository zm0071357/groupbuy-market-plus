package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 组队库存抢占单元测试 - 用内存计数模拟 Redis，验证抢占失败后退单恢复的位置可被再次抢占
 */
public class TeamStockOccupyUnitTest {

    private static final String OCCUPY_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_occupy";
    private static final String RECOVER_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_recovery";
    private static final int TARGET = 3;
    private static final int VALID_TIME = 15;

    private final AtomicLong occupyValue = new AtomicLong();
    private final AtomicLong recoverValue = new AtomicLong();
    private final Set<String> lockedKeys = new HashSet<>();

    private TradeRepositoryImpl tradeRepository;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RAtomicLong occupy = mock(RAtomicLong.class);
        RAtomicLong recover = mock(RAtomicLong.class);
        when(redissonClient.getAtomicLong(OCCUPY_KEY)).thenReturn(occupy);
        when(redissonClient.getAtomicLong(RECOVER_KEY)).thenReturn(recover);
        when(occupy.incrementAndGet()).thenAnswer(invocation -> occupyValue.incrementAndGet());
        when(occupy.decrementAndGet()).thenAnswer(invocation -> occupyValue.decrementAndGet());
        doAnswer(invocation -> {
            occupyValue.set(invocation.getArgument(0));
            return null;
        }).when(occupy).set(anyLong());
        when(recover.get()).thenAnswer(invocation -> recoverValue.get());
        // 按抢占值加锁兜底 - 同一个值只能加锁成功一次
        when(redissonClient.getBucket(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            RBucket<Object> bucket = mock(RBucket.class);
            when(bucket.trySet(any(), anyLong(), any(TimeUnit.class))).thenAnswer(i -> lockedKeys.add(key));
            return bucket;
        });
        tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);
    }

    @Test
    public void test_occupyFailWhenFull_thenRecover_canOccupyAgain() {
        // 目标 3 人，团长已占 1 个位置，成员 A、B 抢占成功后满员
        Assert.assertTrue(occupy());
        Assert.assertTrue(occupy());
        // 满员时成员 C 抢占失败
        Assert.assertFalse(occupy());
        // 成员 B 退单，恢复 1 个位置
        recoverValue.incrementAndGet();
        // 成员 D 应能抢占到恢复的位置
        Assert.assertTrue("退单恢复的位置应可被再次抢占", occupy());
        // 再次满员，成员 E 抢占失败
        Assert.assertFalse(occupy());
    }

    @Test
    public void test_occupyFailRepeatedly_notInflate() {
        Assert.assertTrue(occupy());
        Assert.assertTrue(occupy());
        // 满员时多次抢占失败，抢占量不应虚增
        for (int i = 0; i < 5; i++) {
            Assert.assertFalse(occupy());
        }
        Assert.assertEquals(2, occupyValue.get());
        // 两名成员退单后，可再抢占两个位置
        recoverValue.addAndGet(2);
        Assert.assertTrue(occupy());
        Assert.assertTrue(occupy());
        Assert.assertFalse(occupy());
    }

    private boolean occupy() {
        return tradeRepository.occupyTeamStock(TARGET, VALID_TIME, OCCUPY_KEY, RECOVER_KEY);
    }

}
