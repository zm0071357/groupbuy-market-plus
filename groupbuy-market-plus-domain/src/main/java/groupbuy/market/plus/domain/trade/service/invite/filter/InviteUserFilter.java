package groupbuy.market.plus.domain.trade.service.invite.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.service.invite.factory.InviteLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 邀请人过滤节点 - 是否在拼团中
 */
@Slf4j
@Service
public class InviteUserFilter implements LogicHandler<CheckInviteEntity, InviteLinkFactory.DynamicContext, InviteEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public InviteEntity apply(CheckInviteEntity checkInviteEntity, InviteLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入邀请责任链 - 邀请人过滤节点");
        log.info("邀请责任链 - 邀请人过滤节点，邀请人是否在拼团组队校验。邀请人ID：{}，拼团组队ID：{}", checkInviteEntity.getInviteUserId(), checkInviteEntity.getTeamId());
        Integer count = tradeRepository.checkUserInTeam(checkInviteEntity.getInviteUserId(), checkInviteEntity.getTeamId());
        if (count == 0) {
            throw new AppException(ResponseCodeEnum.E0025.getCode(), ResponseCodeEnum.E0025.getInfo());
        }
        return next(checkInviteEntity, dynamicContext);
    }
}
