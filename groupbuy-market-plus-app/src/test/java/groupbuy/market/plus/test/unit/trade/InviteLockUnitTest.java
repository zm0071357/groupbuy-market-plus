package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.InviteDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.Invite;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 生成邀请码单元测试 - 按「组队 + 用户」加锁，先查后插串行执行，并发时只生成一条有效邀请码
 */
@RunWith(MockitoJUnitRunner.class)
public class InviteLockUnitTest {

    private static final String USER_ID = "user01";
    private static final String TEAM_ID = "TEAM12345678";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private InviteDao inviteDao;

    @Mock
    private GroupBuyTeamDao groupBuyTeamDao;

    @InjectMocks
    private TradeRepositoryImpl tradeRepository;

    @Before
    public void setUp() {
        when(redissonClient.getLock(GroupBuyConstants.InviteUserLockPrefix + TEAM_ID + "_" + USER_ID)).thenReturn(lock);
        lenient().when(groupBuyTeamDao.getTeamById(TEAM_ID)).thenReturn(GroupBuyTeam.builder().teamId(TEAM_ID).endTime(new Date(System.currentTimeMillis() + 600_000)).build());
    }

    @Test
    public void test_queryAndInsertInsideLock() throws Exception {
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        InviteEntity inviteEntity = tradeRepository.invite(USER_ID, TEAM_ID);

        Assert.assertNotNull(inviteEntity.getInviteId());
        InOrder inOrder = inOrder(lock, inviteDao);
        inOrder.verify(lock).tryLock(3, TimeUnit.SECONDS);
        inOrder.verify(inviteDao).getInviteByUserIdWithTeamId(any(Invite.class));
        inOrder.verify(inviteDao).insert(any(Invite.class));
        inOrder.verify(lock).unlock();
    }

    @Test
    public void test_lockNotAcquired_E0031() throws Exception {
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        try {
            tradeRepository.invite(USER_ID, TEAM_ID);
            Assert.fail("应抛出 E0031");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0031.getCode(), e.getCode());
        }
        verifyNoInteractions(inviteDao);
        verify(lock, never()).unlock();
    }

    @Test
    public void test_concurrentInvite_insertOnce() throws Exception {
        // 使用真实互斥锁模拟分布式锁；邀请记录在插入后才可被查到，模拟数据库的先查后插
        ReentrantLock realLock = new ReentrantLock();
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> realLock.tryLock(3, TimeUnit.SECONDS));
        when(lock.isHeldByCurrentThread()).thenAnswer(invocation -> realLock.isHeldByCurrentThread());
        doAnswer(invocation -> { realLock.unlock(); return null; }).when(lock).unlock();
        AtomicReference<Invite> stored = new AtomicReference<>();
        when(inviteDao.getInviteByUserIdWithTeamId(any(Invite.class))).thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            Thread.sleep(20);
            stored.set(invocation.getArgument(0));
            return null;
        }).when(inviteDao).insert(any(Invite.class));

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int i = 0; i < threads; i++) {
                executor.execute(() -> {
                    try {
                        start.await();
                        tradeRepository.invite(USER_ID, TEAM_ID);
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            Assert.assertTrue(done.await(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        verify(inviteDao, times(1)).insert(any(Invite.class));
    }

}
