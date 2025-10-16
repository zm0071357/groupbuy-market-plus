package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
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
 * 拼团组队可用位置过滤节点 - 是否能占有当前拼团组队剩余可用位置（视为库存）
 */
@Slf4j
@Service
public class TeamStockFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckLockResEntity apply(CheckLockEntity checkLockEntity, LockOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入锁单责任链 - 拼团组队可用位置过滤节点");
        // 拼团组队ID为空 - 团长不需要限制
        if (StringUtils.isBlank(checkLockEntity.getTeamId())) {
            log.info("锁单责任链 - 拼团组队可用位置过滤节点，用户为团长，无需抢占可用位置：{}", checkLockEntity.getUserId());
            return CheckLockResEntity.builder()
                    .isHeader(true)
                    .build();
        }
        // 抢占库存
        log.info("锁单责任链 - 拼团组队可用位置过滤节点，尝试抢占可用位置，用户ID：{}", checkLockEntity.getUserId());
        Integer target = dynamicContext.getActivityEntity().getTarget();    // 目标量
        Integer validTime = dynamicContext.getActivityEntity().getValidTime();      // 拼团有效时间
        String teamStockOccupyKey = dynamicContext.getTeamStockOccupyKey(checkLockEntity.getTeamId());      // 抢占Key
        String teamStockRecoverKey = dynamicContext.getTeamStockRecoverKey(checkLockEntity.getTeamId());        // 恢复Key
        boolean status = tradeRepository.occupyTeamStock(target, validTime, teamStockOccupyKey, teamStockRecoverKey);
        // 抢占失败
        if (!status) {
            log.info("锁单责任链 - 拼团组队可用位置过滤节点，抢占可用位置失败，用户ID：{}", checkLockEntity.getUserId());
            throw new AppException(ResponseCodeEnum.E0020.getCode(), ResponseCodeEnum.E0020.getInfo());
        }
        return CheckLockResEntity.builder()
                .isHeader(false)
                .teamStockRecoverKey(teamStockRecoverKey)
                .build();
    }
}
