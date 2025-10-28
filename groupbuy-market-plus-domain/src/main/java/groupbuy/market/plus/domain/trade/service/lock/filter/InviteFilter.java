package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;
import groupbuy.market.plus.domain.trade.model.valobj.InviteStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 邀请码过滤节点 - 邀请码是否可用、是否过期
 */
@Slf4j
@Service
public class InviteFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckLockResEntity apply(CheckLockEntity checkLockEntity, LockOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入锁单责任链 - 邀请码过滤节点");
        if (StringUtils.isNotBlank(checkLockEntity.getInviteId())) {
            log.info("锁单责任链 - 邀请码过滤节点，邀请码可用校验：{}", checkLockEntity.getInviteId());
            InviteEntity inviteEntity = tradeRepository.getInvite(checkLockEntity.getInviteId());
            if (inviteEntity == null) {
                throw new AppException(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode(), ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo());
            }
            dynamicContext.setInviteEntity(inviteEntity);
            if (inviteEntity.getInviteStatusEnum().equals(InviteStatusEnum.UNAVAILABLE) || inviteEntity.getEndTime().before(new Date())) {
                throw new AppException(ResponseCodeEnum.E0026.getCode(), ResponseCodeEnum.E0026.getInfo());
            }
        }
        return next(checkLockEntity, dynamicContext);
    }

}
