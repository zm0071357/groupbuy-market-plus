package groupbuy.market.plus.test.domain.trade;

import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TeamStockRedisIntegrationTest {

    @Test
    public void concurrentRefundsAndReservationsFillBothReleasedPlaces() throws Exception {
        String address = System.getProperty("groupbuy.test.redis.address");
        Assume.assumeTrue("通过 groupbuy.test.redis.address 指定测试 Redis 地址", address != null && !address.isBlank());

        Config config = new Config();
        config.useSingleServer().setAddress(address).setConnectionMinimumIdleSize(1);
        RedissonClient redis = Redisson.create(config);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            TradeRepositoryImpl repository = new TradeRepositoryImpl();
            ReflectionTestUtils.setField(repository, "redissonClient", redis);
            String prefix = "test:team-stock:" + UUID.randomUUID();
            String occupyKey = prefix + ":occupy";
            String recoverKey = prefix + ":recover";

            Assert.assertTrue(repository.occupyTeamStock(3, 15, occupyKey, recoverKey));
            Assert.assertTrue(repository.occupyTeamStock(3, 15, occupyKey, recoverKey));
            Assert.assertFalse(repository.occupyTeamStock(3, 15, occupyKey, recoverKey));

            CountDownLatch start = new CountDownLatch(1);
            Future<Long> firstRefund = executor.submit(() -> {
                start.await();
                return repository.recoverTeamStockOnce(recoverKey, "first", 15);
            });
            Future<Long> secondRefund = executor.submit(() -> {
                start.await();
                return repository.recoverTeamStockOnce(recoverKey, "second", 15);
            });
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                attempts.add(executor.submit(() -> {
                    start.await();
                    return repository.occupyTeamStock(3, 15, occupyKey, recoverKey);
                }));
            }

            start.countDown();
            Assert.assertNotNull(firstRefund.get(10, TimeUnit.SECONDS));
            Assert.assertNotNull(secondRefund.get(10, TimeUnit.SECONDS));
            int filled = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(10, TimeUnit.SECONDS)) {
                    filled++;
                }
            }
            // 恢复前发出的申请允许失败，恢复后剩余空位仍须可以抢占。
            for (int i = 0; i < 2; i++) {
                if (repository.occupyTeamStock(3, 15, occupyKey, recoverKey)) {
                    filled++;
                }
            }

            Assert.assertEquals(2, filled);
            Assert.assertFalse(repository.occupyTeamStock(3, 15, occupyKey, recoverKey));
            Assert.assertEquals(4L, redis.getAtomicLong(occupyKey).get());
            Assert.assertTrue(redis.getAtomicLong(occupyKey).remainTimeToLive() > 0);
            Assert.assertTrue(redis.getAtomicLong(recoverKey).remainTimeToLive() > 0);
        } finally {
            executor.shutdownNow();
            redis.shutdown();
        }
    }
}
