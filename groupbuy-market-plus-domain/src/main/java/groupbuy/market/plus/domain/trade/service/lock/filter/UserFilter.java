package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户过滤节点 - 用户可参与次数、用户是否参与自身拼团
 */
@Slf4j
@Service
public class UserFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckLockResEntity apply(CheckLockEntity checkLockEntity, LockOrderLinkFactory.DynamicContext dynamicContext) {
        log.info("进入锁单责任链 - 用户过滤节点");
        // 校验用户可参与次数
        log.info("锁单责任链 - 用户过滤节点，校验用户可参与次数，用户ID：{}", checkLockEntity.getUserId());
        Integer userTakeActivityCount = tradeRepository.checkUserTakeActivityCount(checkLockEntity.getActivityId(), checkLockEntity.getUserId());
        if (userTakeActivityCount > dynamicContext.getActivityEntity().getTakeLimitCount()) {
            throw new AppException(ResponseCodeEnum.E0008.getCode(), ResponseCodeEnum.E0008.getInfo());
        }
        // 校验用户是否参与自身拼团
        if (!StringUtils.isBlank(checkLockEntity.getTeamId())) {
            log.info("锁单责任链 - 用户过滤节点，校验用户是否参与自身拼团，用户ID：{}", checkLockEntity.getUserId());
            Integer userTakeTeamCount = tradeRepository.checkUserTakeTeamCount(checkLockEntity.getTeamId(), checkLockEntity.getUserId());
            if (userTakeTeamCount > 0) {
                throw new AppException(ResponseCodeEnum.E0009.getCode(), ResponseCodeEnum.E0009.getInfo());
            }
        }
        return CheckLockResEntity.builder()
                .isHeader(StringUtils.isBlank(checkLockEntity.getTeamId()))
                .build();
    }
}
