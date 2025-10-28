package groupbuy.market.plus.domain.trade.service.invite.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.invite.factory.InviteLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 拼团组队过滤节点 - 拼团是否完成
 */
@Slf4j
@Service
public class TeamStatusFilter implements LogicHandler<CheckInviteEntity, InviteLinkFactory.DynamicContext, InviteEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public InviteEntity apply(CheckInviteEntity checkInviteEntity, InviteLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入邀请责任链 - 拼团组队过滤节点");
        log.info("邀请责任链 - 拼团组队过滤节点，拼团组队状态校验。邀请人ID：{}，拼团组队ID：{}", checkInviteEntity.getInviteUserId(), checkInviteEntity.getTeamId());
        TeamProgressVO teamProgressVO = tradeRepository.getTeamProgress(checkInviteEntity.getTeamId());
        if (!teamProgressVO.getStatus().equals(TeamStatusEnum.PROGRESS.getStatus())) {
            throw new AppException(ResponseCodeEnum.E0023.getCode(), ResponseCodeEnum.E0023.getInfo());
        }
        return tradeRepository.invite(checkInviteEntity.getInviteUserId(), checkInviteEntity.getTeamId());
    }

}
