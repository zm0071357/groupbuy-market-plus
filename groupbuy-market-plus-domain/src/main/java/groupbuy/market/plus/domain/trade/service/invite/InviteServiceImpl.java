package groupbuy.market.plus.domain.trade.service.invite;

import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.service.invite.factory.InviteLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
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
        List<String> timeoutTeamIdList = tradeRepository.getTimeoutTeamIdList();
        tradeRepository.inviteExpire(timeoutTeamIdList);
    }

}
