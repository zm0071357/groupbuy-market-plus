package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.service.invite.InviteService;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import groupbuy.market.plus.trigger.job.TeamTimeOutRefundJob;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 超时组队标记失败单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class TeamTimeoutFailUnitTest {

    @Mock
    private RefundOrderService refundOrderService;

    @Mock
    private InviteService inviteService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private TeamTimeOutRefundJob job;

    /**
     * 执行顺序：超时退单 → 邀请码失效 → 标记组队失败
     */
    @Test
    public void test_job_order() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(refundOrderService.teamTimeoutRefund()).thenReturn(Collections.emptyMap());
        when(refundOrderService.teamTimeoutFail()).thenReturn(1);

        job.exec();

        InOrder inOrder = inOrder(refundOrderService, inviteService);
        inOrder.verify(refundOrderService).teamTimeoutRefund();
        inOrder.verify(inviteService).inviteExpire();
        inOrder.verify(refundOrderService).teamTimeoutFail();
    }

    /**
     * 只有超时、拼单中且已无未退单订单的组队才会被标记为失败
     */
    @Test
    public void test_sql_onlyAllRefunded() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mybatis/mapper/group_buy_team_mapper.xml";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String sql = configuration.getMappedStatement("groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.updateTimeoutTeamFail")
                .getBoundSql(null).getSql().replaceAll("\\s+", " ");

        Assert.assertTrue(sql, sql.contains("set t.status = 2"));
        Assert.assertTrue(sql, sql.contains("where t.status = 0 and now() > t.end_time"));
        Assert.assertTrue(sql, sql.contains("not exists ( select 1 from group_buy_team_order o where o.team_id = t.team_id and o.status in (0, 1) )"));
    }

}
