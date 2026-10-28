package groupbuy.market.plus.domain.trade.service.invite;

import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.service.invite.factory.InviteLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class InviteServiceImpl implements InviteService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private BusinessLinkedList<CheckInviteEntity, InviteLinkFactory.DynamicContext, InviteEntity> inviteLink;

    @Override
    public InviteEntity invite(String userId, String teamId) throws Exception {
        // 责任链过滤并生成邀请码
        CheckInviteEntity checkInviteEntity = CheckInviteEntity.builder()
                .inviteUserId(userId)
                .teamId(teamId)
                .build();
        return inviteLink.apply(checkInviteEntity, new InviteLinkFactory.DynamicContext());
    }

    @Override
    public void inviteExpire() {
        // 按组队ID游标分批处理超时组队，避免积压时生成超长 IN 语句
        String lastTeamId = null;
        while (true) {
            List<String> timeoutTeamIdList = tradeRepository.getTimeoutTeamIdList(lastTeamId, GroupBuyConstants.TimeoutTeamPageSize);
            // 没有超时组队时直接返回，避免生成 where team_id in () 非法 SQL
            if (timeoutTeamIdList == null || timeoutTeamIdList.isEmpty()) {
                return;
            }
            tradeRepository.inviteExpire(timeoutTeamIdList);
            if (timeoutTeamIdList.size() < GroupBuyConstants.TimeoutTeamPageSize) {
                return;
            }
            lastTeamId = timeoutTeamIdList.get(timeoutTeamIdList.size() - 1);
        }
    }

}
