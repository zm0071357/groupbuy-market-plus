package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.service.lock.CustomerEligibilityService;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/** 在邀请码解析和组队校验后，验证老带新开团或参团资格。 */
@Service
public class CustomerEligibilityFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private CustomerEligibilityService customerEligibilityService;

    @Override
    public CheckLockResEntity apply(CheckLockEntity request, LockOrderLinkFactory.DynamicContext context) throws Exception {
        GroupBuyTeamEntity team = context.getJoinedTeam();
        String shopId = team == null ? request.getEligibilityShopId() : team.getEligibilityShopId();
        if (shopId != null) {
            ResponseCodeEnum rejection = customerEligibilityService
                    .qualify(shopId, request.getUserId(), request.getOutTradeNo()).rejection(team != null);
            if (rejection != null) {
                throw new AppException(rejection.getCode(), rejection.getInfo());
            }
        }
        return next(request, context);
    }
}
