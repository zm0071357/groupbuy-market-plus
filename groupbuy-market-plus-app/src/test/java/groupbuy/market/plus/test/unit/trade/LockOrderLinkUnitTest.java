package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.ActivityStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.InviteStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.lock.filter.*;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 锁单责任链单元测试 - 使用真实过滤节点组装，仅模拟仓储
 * 覆盖：仅凭邀请码参团、邀请码与组队不一致、组队过期 / 已完成 / 不属于本活动、通过自己的邀请码参加自己所在的组队
 */
@RunWith(MockitoJUnitRunner.class)
public class LockOrderLinkUnitTest {

    private static final Long ACTIVITY_ID = 100123L;
    private static final String TEAM_ID = "TEAM12345678";
    private static final String INVITE_ID = "INVITE001";
    private static final String USER_ID = "user02";

    @Mock
    private TradeRepository tradeRepository;

    private BusinessLinkedList<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> lockOrderLink;

    @Before
    public void setUp() {
        ActivityFilter activityFilter = new ActivityFilter();
        InviteFilter inviteFilter = new InviteFilter();
        TeamFilter teamFilter = new TeamFilter();
        UserFilter userFilter = new UserFilter();
        TeamStockFilter teamStockFilter = new TeamStockFilter();
        for (Object filter : new Object[]{activityFilter, inviteFilter, teamFilter, userFilter, teamStockFilter}) {
            ReflectionTestUtils.setField(filter, "tradeRepository", tradeRepository);
        }
        lockOrderLink = new LockOrderLinkFactory().lockOrderLink(activityFilter, inviteFilter, teamFilter, userFilter, teamStockFilter);

        ActivityEntity activityEntity = new ActivityEntity();
        activityEntity.setActivityId(ACTIVITY_ID);
        activityEntity.setActivityStatusEnum(ActivityStatusEnum.EFFECTIVE);
        activityEntity.setStartTime(new Date(System.currentTimeMillis() - 3600_000));
        activityEntity.setEndTime(new Date(System.currentTimeMillis() + 3600_000));
        activityEntity.setTakeLimitCount(10);
        activityEntity.setTarget(3);
        activityEntity.setValidTime(15);
        lenient().when(tradeRepository.getActivityById(ACTIVITY_ID)).thenReturn(activityEntity);
        lenient().when(tradeRepository.checkUserTakeActivityCount(ACTIVITY_ID, USER_ID)).thenReturn(0);
        lenient().when(tradeRepository.getInvite(INVITE_ID)).thenReturn(InviteEntity.builder()
                .inviteId(INVITE_ID)
                .inviteUserId("user01")
                .teamId(TEAM_ID)
                .inviteStatusEnum(InviteStatusEnum.AVAILABLE)
                .endTime(new Date(System.currentTimeMillis() + 600_000))
                .build());
        lenient().when(tradeRepository.getTeamById(TEAM_ID)).thenReturn(buildTeam(ACTIVITY_ID, TeamStatusEnum.PROGRESS, 600_000));
        lenient().when(tradeRepository.occupyTeamStock(anyInt(), anyInt(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    public void test_inviteOnly_joinInviteTeam() throws Exception {
        when(tradeRepository.checkUserTakeTeamCount(TEAM_ID, USER_ID)).thenReturn(0);
        CheckLockEntity checkLockEntity = buildCheckLock(null, INVITE_ID);

        CheckLockResEntity result = lockOrderLink.apply(checkLockEntity, new LockOrderLinkFactory.DynamicContext());

        // 组队由邀请码补全，按团员参团并记录邀请人
        Assert.assertEquals(TEAM_ID, checkLockEntity.getTeamId());
        Assert.assertFalse(result.getIsHeader());
        Assert.assertEquals("user01", result.getInviteUserId());
        Assert.assertEquals(LockOrderLinkFactory.getTeamStockRecoverKey(ACTIVITY_ID, TEAM_ID), result.getTeamStockRecoverKey());
    }

    @Test
    public void test_inviteTeamMismatch_reject() {
        assertReject(buildCheckLock("TEAM87654321", INVITE_ID), ResponseCodeEnum.E0028);
        verify(tradeRepository, never()).occupyTeamStock(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    public void test_selfInvite_rejectByUserFilter() {
        // 用户已在该组队中，仅凭邀请码再次参团同样被「不可参与同一个组队」拦截
        when(tradeRepository.checkUserTakeTeamCount(TEAM_ID, USER_ID)).thenReturn(1);
        assertReject(buildCheckLock(null, INVITE_ID), ResponseCodeEnum.E0009);
    }

    @Test
    public void test_teamExpired_reject() {
        when(tradeRepository.getTeamById(TEAM_ID)).thenReturn(buildTeam(ACTIVITY_ID, TeamStatusEnum.PROGRESS, -1000));
        assertReject(buildCheckLock(TEAM_ID, null), ResponseCodeEnum.E0029);
    }

    @Test
    public void test_teamCompleted_reject() {
        when(tradeRepository.getTeamById(TEAM_ID)).thenReturn(buildTeam(ACTIVITY_ID, TeamStatusEnum.COMPLETE, 600_000));
        assertReject(buildCheckLock(TEAM_ID, null), ResponseCodeEnum.E0029);
    }

    @Test
    public void test_teamOfOtherActivity_reject() {
        when(tradeRepository.getTeamById(TEAM_ID)).thenReturn(buildTeam(999999L, TeamStatusEnum.PROGRESS, 600_000));
        assertReject(buildCheckLock(TEAM_ID, null), ResponseCodeEnum.E0030);
    }

    @Test
    public void test_teamNotExist_reject() {
        when(tradeRepository.getTeamById(TEAM_ID)).thenReturn(null);
        assertReject(buildCheckLock(TEAM_ID, null), ResponseCodeEnum.E0015);
    }

    @Test
    public void test_header_noTeamCheck() throws Exception {
        CheckLockResEntity result = lockOrderLink.apply(buildCheckLock(null, null), new LockOrderLinkFactory.DynamicContext());

        Assert.assertTrue(result.getIsHeader());
        verify(tradeRepository, never()).getTeamById(anyString());
    }

    private void assertReject(CheckLockEntity checkLockEntity, ResponseCodeEnum expected) {
        try {
            lockOrderLink.apply(checkLockEntity, new LockOrderLinkFactory.DynamicContext());
            Assert.fail("应抛出 " + expected.getCode());
        } catch (AppException e) {
            Assert.assertEquals(expected.getCode(), e.getCode());
        } catch (Exception e) {
            Assert.fail("应抛出 AppException，实际：" + e);
        }
    }

    private CheckLockEntity buildCheckLock(String teamId, String inviteId) {
        return CheckLockEntity.builder()
                .activityId(ACTIVITY_ID)
                .userId(USER_ID)
                .teamId(teamId)
                .inviteId(inviteId)
                .build();
    }

    private GroupBuyTeamEntity buildTeam(Long activityId, TeamStatusEnum teamStatusEnum, long endOffsetMillis) {
        return GroupBuyTeamEntity.builder()
                .teamId(TEAM_ID)
                .activityId(activityId)
                .teamStatusEnum(teamStatusEnum)
                .startTime(new Date(System.currentTimeMillis() - 600_000))
                .endTime(new Date(System.currentTimeMillis() + endOffsetMillis))
                .build();
    }

}
