package groupbuy.market.plus.domain.trade.service.invite.factory;

import groupbuy.market.plus.domain.trade.model.entity.CheckInviteEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.service.invite.filter.InviteUserFilter;
import groupbuy.market.plus.domain.trade.service.invite.filter.TeamStatusFilter;
import groupbuy.market.plus.types.design.framework.link.multition.LinkArmory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 邀请责任链工厂
 */
@Service
public class InviteLinkFactory {


    @Bean("inviteLink")
    public BusinessLinkedList<CheckInviteEntity, DynamicContext, InviteEntity> inviteLink(InviteUserFilter inviteUserFilter, TeamStatusFilter teamStatusFilter) {
        LinkArmory<CheckInviteEntity, DynamicContext, InviteEntity> linkArmory = new LinkArmory<>("邀请责任链", inviteUserFilter, teamStatusFilter);
        return linkArmory.getLogicLink();
    }

    /**
     * 动态上下文信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class DynamicContext {

    }

}
