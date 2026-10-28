package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 锁单服务单元测试 - 仅凭邀请码参团时，落库使用责任链补全的组队ID
 */
@RunWith(MockitoJUnitRunner.class)
public class LockOrderTeamIdWriteBackUnitTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private BusinessLinkedList<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> lockOrderLink;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private LockOrderServiceImpl lockOrderService;

    @Test
    public void test_inviteOnly_teamIdWriteBack() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        // 模拟邀请码过滤节点补全组队ID
        when(lockOrderLink.apply(any(CheckLockEntity.class), any(LockOrderLinkFactory.DynamicContext.class))).thenAnswer(invocation -> {
            CheckLockEntity checkLockEntity = invocation.getArgument(0);
            checkLockEntity.setTeamId("TEAM12345678");
            return CheckLockResEntity.builder().isHeader(false).inviteUserId("user01").build();
        });
        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder().activityId(100123L).build();

        lockOrderService.lockOrder(UserEntity.builder().userId("user02").inviteId("INVITE001").build(),
                groupBuyTeamEntity, OrderDetailEntity.builder().outTradeNo("OUT0001").build());

        ArgumentCaptor<LockOrderAggregate> captor = ArgumentCaptor.forClass(LockOrderAggregate.class);
        verify(tradeRepository).lockOrder(captor.capture());
        Assert.assertEquals("TEAM12345678", captor.getValue().getGroupBuyTeamEntity().getTeamId());
    }

}
