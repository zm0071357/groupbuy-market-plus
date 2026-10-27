package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 仓储按订单幂等恢复库存单元测试 - 校验 Lua 脚本入参与返回值转换（脚本本身需连接 Redis 验证）
 */
@RunWith(MockitoJUnitRunner.class)
public class RecoverTeamStockRepositoryUnitTest {

    private static final String RECOVER_KEY = "groupbuy_market_plus_stock_key_100123_TEAM12345678_recovery";
    private static final String ORDER_ID = "ORDER000000000001";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RScript script;

    @InjectMocks
    private TradeRepositoryImpl tradeRepository;

    @Before
    public void setUp() {
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
    }

    @Test
    public void test_firstRecover_returnCount() {
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any())).thenReturn(3L);

        Long count = tradeRepository.recoverTeamStockOnce(RECOVER_KEY, ORDER_ID, 15);

        Assert.assertEquals(Long.valueOf(3), count);
        // 防重标记Key在前、恢复Key在后；过期时间 = (15 + 60) 分钟
        verify(script).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                eq(Arrays.asList(RECOVER_KEY + "_" + ORDER_ID, RECOVER_KEY)), eq(String.valueOf(75 * 60 * 1000L)));
    }

    @Test
    public void test_repeatRecover_returnNull() {
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER), anyList(), any())).thenReturn(-1L);

        Assert.assertNull(tradeRepository.recoverTeamStockOnce(RECOVER_KEY, ORDER_ID, 15));
    }

}
