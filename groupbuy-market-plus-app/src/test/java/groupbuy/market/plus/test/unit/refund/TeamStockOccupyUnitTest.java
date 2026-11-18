package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存抢占通过一个 Redis 脚本处理，失败时仓储不执行额外的回减。
 */
public class TeamStockOccupyUnitTest {

    private static final String OCCUPY_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_occupy";
    private static final String RECOVER_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_recovery";
    private static final String EXPIRE_MILLIS = "4500000";

    private RedissonClient redissonClient;
    private RScript script;
    private TradeRepositoryImpl tradeRepository;

    @Before
    public void setUp() {
        redissonClient = mock(RedissonClient.class);
        script = mock(RScript.class);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "redissonClient", redissonClient);
    }

    @Test
    public void test_occupySuccess_singleScriptWithCountersAndExpiry() {
        stubScriptResult(1L);

        Assert.assertTrue(occupy());

        verifySingleRedisOperation();
    }

    @Test
    public void test_occupyRejected_noSeparateDecrement() {
        stubScriptResult(0L);

        Assert.assertFalse(occupy());

        verifySingleRedisOperation();
    }

    private void stubScriptResult(long result) {
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                eq(Arrays.asList(OCCUPY_KEY, RECOVER_KEY)), eq("3"), eq(EXPIRE_MILLIS))).thenReturn(result);
    }

    private void verifySingleRedisOperation() {
        verify(redissonClient).getScript(StringCodec.INSTANCE);
        verify(script).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                eq(Arrays.asList(OCCUPY_KEY, RECOVER_KEY)), eq("3"), eq(EXPIRE_MILLIS));
        verifyNoMoreInteractions(redissonClient, script);
    }

    private boolean occupy() {
        return tradeRepository.occupyTeamStock(3, 15, OCCUPY_KEY, RECOVER_KEY);
    }
}
