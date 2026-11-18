package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.ActivityStatusEnum;
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
 * 活动过滤节点 - 活动有效性、时间
 */
@Slf4j
@Service
public class ActivityFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckLockResEntity apply(CheckLockEntity checkLockEntity, LockOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入锁单责任链 - 活动过滤节点");
        ActivityEntity activityEntity = tradeRepository.getActivityById(checkLockEntity.getActivityId());
        if (activityEntity == null) {
            throw new AppException(ResponseCodeEnum.E0010.getCode(), ResponseCodeEnum.E0010.getInfo());
        }
        // 校验活动有效性
        log.info("锁单责任链 - 活动过滤节点，活动有效性校验，活动ID：{}", activityEntity.getActivityId());
        if (!ActivityStatusEnum.EFFECTIVE.equals(activityEntity.getActivityStatusEnum())) {
            throw new AppException(ResponseCodeEnum.E0006.getCode(), ResponseCodeEnum.E0006.getInfo());
        }
        // 校验活动时间
        log.info("锁单责任链 - 活动过滤节点，活动时间校验，活动ID：{}", activityEntity.getActivityId());
        Date currentDate = new Date();
        // 运营活动关闭开团后，已有组队按自己的有效期继续参团。
        boolean joining = StringUtils.isNotBlank(checkLockEntity.getTeamId()) || StringUtils.isNotBlank(checkLockEntity.getInviteId());
        Boolean openingEnabled = activityEntity.getOpeningEnabled();
        if (Boolean.FALSE.equals(openingEnabled) && !joining) {
            throw new AppException(ResponseCodeEnum.E0038.getCode(), ResponseCodeEnum.E0038.getInfo());
        }
        if ((openingEnabled == null || !joining)
                && (currentDate.before(activityEntity.getStartTime()) || currentDate.after(activityEntity.getEndTime()))) {
            throw new AppException(ResponseCodeEnum.E0007.getCode(), ResponseCodeEnum.E0007.getInfo());
        }
        // 活动写入上下文
        dynamicContext.setActivityEntity(activityEntity);
        return next(checkLockEntity, dynamicContext);
    }
}
