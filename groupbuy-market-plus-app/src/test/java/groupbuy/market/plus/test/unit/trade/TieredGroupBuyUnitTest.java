package groupbuy.market.plus.test.unit.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import groupbuy.market.plus.api.dto.IndexRequestDTO;
import groupbuy.market.plus.api.dto.IndexResponseDTO;
import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import groupbuy.market.plus.domain.trade.model.valobj.ActivityStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.InviteStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.lock.filter.*;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.adapter.repository.SkipGroupLedger;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import groupbuy.market.plus.trigger.http.IndexController;
import groupbuy.market.plus.trigger.http.TradeController;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** 从商品页、锁单入口到结算仓储验证档位与组队存档，使用模拟 DAO 和 Redis。 */
public class TieredGroupBuyUnitTest {

    private static final Long ACTIVITY_ID = 100123L;
    private static final String GOODS_ID = "9890001";
    private static final String SOURCE = "s01";
    private static final String CHANNEL = "c01";
    private static final List<GroupBuyTierVO> TIERS = List.of(tier(5, 5, "69"), tier(3, 3, "79"));

    private final Map<String, GroupBuyTeam> teams = new HashMap<>();
    private final GroupBuyTeamOrderDao orderDao = mock(GroupBuyTeamOrderDao.class);
    private final NotifyTaskDao notifyTaskDao = mock(NotifyTaskDao.class);
    private final TradeRepositoryImpl repository = spy(new TradeRepositoryImpl());
    private final IndexGroupBuyMarketService trialService = mock(IndexGroupBuyMarketService.class);
    private final TradeController tradeController = new TradeController();

    @Before
    public void setUp() throws Exception {
        GroupBuyTeamDao teamDao = mock(GroupBuyTeamDao.class);
        ReflectionTestUtils.setField(repository, "groupBuyTeamDao", teamDao);
        ReflectionTestUtils.setField(repository, "groupBuyTeamOrderDao", orderDao);
        ReflectionTestUtils.setField(repository, "notifyTaskDao", notifyTaskDao);
        ReflectionTestUtils.setField(repository, "skipGroupLedger", mock(SkipGroupLedger.class));

        // 模拟数据库保存组队，让开团、参团及结算使用同一份持久化状态。
        doAnswer(call -> {
            GroupBuyTeam team = call.getArgument(0);
            team.setStatus(0);
            teams.put(team.getTeamId(), team);
            return null;
        }).when(teamDao).insert(any());
        when(teamDao.getTeamById(anyString())).thenAnswer(call -> teams.get(call.getArgument(0)));
        when(teamDao.getTeamProgress(anyString())).thenAnswer(call -> teams.get(call.getArgument(0)));
        when(teamDao.updateLockCount(anyString())).thenAnswer(call -> {
            GroupBuyTeam team = teams.get(call.getArgument(0));
            if (team.getLockCount() >= team.getTargetCount()) {
                return 0;
            }
            team.setLockCount(team.getLockCount() + 1);
            return 1;
        });
        when(teamDao.updateTeamAddCompleteCount(anyString())).thenAnswer(call -> {
            GroupBuyTeam team = teams.get(call.getArgument(0));
            team.setCompleteCount(team.getCompleteCount() + 1);
            return 1;
        });
        when(teamDao.updateTeamStatusComplete(anyString())).thenAnswer(call -> {
            GroupBuyTeam team = teams.get(call.getArgument(0));
            if (team.getStatus() != 0 || !team.getCompleteCount().equals(team.getTargetCount())) {
                return 0;
            }
            team.setStatus(1);
            return 1;
        });
        when(orderDao.updateOrderStatusComplete(any())).thenReturn(1);
        when(orderDao.getCompleteTeamOutTradeNoList(anyString())).thenReturn(List.of());

        Date start = new Date(System.currentTimeMillis() - 60_000);
        Date end = new Date(System.currentTimeMillis() + 3_600_000);
        doReturn(ActivityEntity.builder().activityId(ACTIVITY_ID).activityStatusEnum(ActivityStatusEnum.EFFECTIVE)
                .startTime(start).endTime(end).target(2).validTime(15).takeLimitCount(1).build())
                .when(repository).getActivityById(ACTIVITY_ID);
        doReturn(0).when(repository).checkUserTakeActivityCount(eq(ACTIVITY_ID), anyString());
        doReturn(0).when(repository).checkUserTakeTeamCount(anyString(), anyString());
        doReturn(true).when(repository).occupyTeamStock(anyInt(), anyInt(), anyString(), anyString());

        ActivityFilter activity = new ActivityFilter();
        InviteFilter invite = new InviteFilter();
        TeamFilter team = new TeamFilter();
        UserFilter user = new UserFilter();
        TeamStockFilter stock = new TeamStockFilter();
        for (Object filter : List.of(activity, invite, team, user, stock)) {
            ReflectionTestUtils.setField(filter, "tradeRepository", repository);
        }
        RLock lock = mock(RLock.class);
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        RedissonClient redis = mock(RedissonClient.class);
        when(redis.getLock(anyString())).thenReturn(lock);
        LockOrderServiceImpl lockService = new LockOrderServiceImpl();
        ReflectionTestUtils.setField(lockService, "tradeRepository", repository);
        ReflectionTestUtils.setField(lockService, "redissonClient", redis);
        ReflectionTestUtils.setField(lockService, "lockOrderLink", new LockOrderLinkFactory()
                .lockOrderLink(activity, invite, team, user, new CustomerEligibilityFilter(), stock));
        ReflectionTestUtils.setField(tradeController, "lockOrderService", lockService);
        ReflectionTestUtils.setField(tradeController, "indexGroupBuyMarketService", trialService);
        setTrial(TIERS, "100");
    }

    @Test
    public void displaysBothTiersAndSettlesAtTheirOwnTargetCounts() throws Exception {
        IndexController index = new IndexController();
        ReflectionTestUtils.setField(index, "indexGroupBuyMarketService", trialService);
        when(trialService.getStatistics(ACTIVITY_ID, GOODS_ID)).thenReturn(StatisticsVO.builder().build());
        IndexRequestDTO request = new ObjectMapper().readValue("""
                {"userId":"buyer","goodsId":"%s","source":"%s","channel":"%s"}
                """.formatted(GOODS_ID, SOURCE, CHANNEL), IndexRequestDTO.class);
        List<IndexResponseDTO.Tier> tiers = index.index(request).getData().getTiers();
        assertEquals(List.of(3L, 5L), tiers.stream().map(IndexResponseDTO.Tier::getTierId).toList());
        assertEquals(new BigDecimal("75.05"), tiers.get(0).getLeaderPrice());
        assertEquals(new BigDecimal("65.55"), tiers.get(1).getLeaderPrice());

        for (IndexResponseDTO.Tier tier : tiers) {
            String prefix = "tier" + tier.getTierId();
            LockOrderResponseDTO leader = lock(prefix + "-0", null, null, tier.getTierId());
            GroupBuyTeam team = teams.get(leader.getTeamId());
            assertEquals(tier.getTierId(), team.getTierId());
            assertEquals(tier.getTargetCount(), team.getTargetCount());
            assertEquals(tier.getLeaderPrice(), leader.getPayPrice());

            for (int member = 1; member < tier.getTargetCount(); member++) {
                LockOrderResponseDTO result = lock(prefix + "-" + member, team.getTeamId(), null, null);
                assertEquals(tier.getMemberPrice(), result.getPayPrice());
            }

            for (int member = 0; member < tier.getTargetCount(); member++) {
                boolean shouldComplete = member == tier.getTargetCount() - 1;
                assertEquals(shouldComplete, repository.settleOrder(SettleOrderAggregate.builder()
                        .userEntity(UserEntity.builder().userId(prefix + "-" + member).build())
                        .groupBuyTeamEntity(repository.getTeamById(team.getTeamId()))
                        .orderPaySuccessEntity(OrderPaySuccessEntity.builder().outTradeNo(prefix + "-" + member)
                                .outTradeNoPayTime(new Date()).build())
                        .build()) != null);
            }
        }
        verify(notifyTaskDao, times(2)).insert(any(NotifyTask.class));
        verify(repository, times(2)).occupyTeamStock(eq(3), eq(15), anyString(), anyString());
        verify(repository, times(4)).occupyTeamStock(eq(5), eq(15), anyString(), anyString());
    }

    @Test
    public void inviteJoinKeepsOriginalPriceAndCountAfterTierEdit() throws Exception {
        LockOrderResponseDTO leader = lock("leader", null, null, 3L);
        doReturn(InviteEntity.builder().teamId(leader.getTeamId()).inviteUserId("leader")
                .inviteStatusEnum(InviteStatusEnum.AVAILABLE).endTime(new Date(System.currentTimeMillis() + 60_000))
                .build()).when(repository).getInvite("invite");
        setTrial(List.of(tier(3, 4, "89"), tier(5, 6, "76")), "110");

        LockOrderResponseDTO member = lock("member", null, "invite", null);

        assertEquals(leader.getTeamId(), member.getTeamId());
        assertEquals(new BigDecimal("100"), member.getOriginalPrice());
        assertEquals(new BigDecimal("79.00"), member.getPayPrice());
        verify(repository).occupyTeamStock(eq(3), eq(15), anyString(), anyString());

        GroupBuyTeamOrder order = GroupBuyTeamOrder.builder().teamId(member.getTeamId())
                .orderId(member.getOrderId()).originalPrice(member.getOriginalPrice()).payPrice(member.getPayPrice())
                .isHeader(0).status(0).build();
        when(orderDao.getNoPayLockOrderByOutTradeNo(any())).thenReturn(order);
        assertEquals(new BigDecimal("79.00"), lock("member", null, "invite", null).getPayPrice());
        verify(repository, times(1)).occupyTeamStock(anyInt(), anyInt(), anyString(), anyString());

        when(orderDao.getNoPayLockOrderByOutTradeNo(any())).thenReturn(null);
        setTrial(List.of(), "110");
        assertEquals(new BigDecimal("79.00"), lock("last-member", leader.getTeamId(), null, 3L).getPayPrice());
        verify(repository, times(2)).occupyTeamStock(eq(3), eq(15), anyString(), anyString());
    }

    @Test
    public void rejectsInvalidSelectionsBeforeReservingStockAndKeepsOrdinaryGroups() throws Exception {
        assertEquals("E0035", tradeController.lockOrder(request("missing", null, null, null)).getCode());
        assertEquals("E0035", tradeController.lockOrder(request("invalid", null, null, 9L)).getCode());

        LockOrderResponseDTO leader = lock("leader", null, null, 5L);
        assertEquals("E0035", tradeController.lockOrder(request("member", leader.getTeamId(), null, 3L)).getCode());
        LockOrderRequestDTO otherChannel = request("member", null, null, null);
        otherChannel.setTeamId(leader.getTeamId());
        otherChannel.setChannel("other");
        assertEquals("E0037", tradeController.lockOrder(otherChannel).getCode());
        verify(repository, never()).occupyTeamStock(anyInt(), anyInt(), anyString(), anyString());

        setTrial(List.of(), "100");
        LockOrderResponseDTO ordinary = lock("ordinary", null, null, null);
        GroupBuyTeam team = teams.get(ordinary.getTeamId());
        assertNull(team.getTierId());
        assertEquals(Integer.valueOf(2), team.getTargetCount());
        assertEquals(new BigDecimal("85.50"), ordinary.getPayPrice());
        assertEquals(new BigDecimal("90"), lock("ordinary-member", ordinary.getTeamId(), null, null).getPayPrice());
    }

    private void setTrial(List<GroupBuyTierVO> tiers, String originalPrice) throws Exception {
        when(trialService.indexMarketTrial(any())).thenReturn(TrialBalanceEntity.builder()
                .activityVO(ActivityVO.builder().activityId(ACTIVITY_ID).validTime(15).target(2).tiers(tiers).build())
                .goodsId(GOODS_ID).originalPrice(new BigDecimal(originalPrice)).payPrice(new BigDecimal("90"))
                .isEnable(true).build());
    }

    private LockOrderResponseDTO lock(String user, String teamId, String inviteId, Long tierId) {
        Response<LockOrderResponseDTO> response = tradeController.lockOrder(request(user, teamId, inviteId, tierId));
        assertEquals(response.getInfo(), "0000", response.getCode());
        return response.getData();
    }

    private LockOrderRequestDTO request(String user, String teamId, String inviteId, Long tierId) {
        LockOrderRequestDTO request = new LockOrderRequestDTO();
        request.setUserId(user);
        request.setOutTradeNo(user);
        request.setGoodsId(GOODS_ID);
        request.setActivityId(ACTIVITY_ID);
        request.setTeamId(teamId);
        request.setInviteId(inviteId);
        request.setTierId(tierId);
        request.setSource(SOURCE);
        request.setChannel(CHANNEL);
        LockOrderRequestDTO.NotifyConfig notify = new LockOrderRequestDTO.NotifyConfig();
        notify.setNotifyType(1);
        notify.setNotifyUrl("http://localhost/notify");
        request.setNotifyConfig(notify);
        return request;
    }

    private static GroupBuyTierVO tier(long id, int target, String price) {
        return new GroupBuyTierVO(id, target, new BigDecimal(price));
    }
}
