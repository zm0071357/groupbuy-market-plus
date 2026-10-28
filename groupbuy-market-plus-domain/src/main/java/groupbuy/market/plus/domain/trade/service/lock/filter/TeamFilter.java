package groupbuy.market.plus.domain.trade.service.lock.filter;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.handler.LogicHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

/**
 * 组队过滤节点 - 参团时校验组队存在、属于本活动、拼单中且在有效期内
 */
@Slf4j
@Service
public class TeamFilter implements LogicHandler<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> {

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public CheckLockResEntity apply(CheckLockEntity checkLockEntity, LockOrderLinkFactory.DynamicContext dynamicContext) throws Exception {
        log.info("进入锁单责任链 - 组队过滤节点");

        // 组队ID为空 - 团长开团，无需校验
        if (StringUtils.isBlank(checkLockEntity.getTeamId())) {
            return next(checkLockEntity, dynamicContext);
        }

        log.info("锁单责任链 - 组队过滤节点，校验组队有效性，用户ID：{}，组队ID：{}", checkLockEntity.getUserId(), checkLockEntity.getTeamId());
        GroupBuyTeamEntity groupBuyTeamEntity = tradeRepository.getTeamById(checkLockEntity.getTeamId());
        if (groupBuyTeamEntity == null) {
            throw new AppException(ResponseCodeEnum.E0015.getCode(), ResponseCodeEnum.E0015.getInfo());
        }
        // 组队必须属于本次锁单的活动，且商品、来源、渠道一致 - 同一活动可绑定多个商品或渠道，避免跨商品参团混入组队
        if (!checkLockEntity.getActivityId().equals(groupBuyTeamEntity.getActivityId())
                || !Objects.equals(checkLockEntity.getGoodsId(), groupBuyTeamEntity.getGoodsId())
                || !Objects.equals(checkLockEntity.getSource(), groupBuyTeamEntity.getSource())
                || !Objects.equals(checkLockEntity.getChannel(), groupBuyTeamEntity.getChannel())) {
            log.info("锁单责任链 - 组队过滤节点，组队与本次锁单的活动商品不匹配，用户ID：{}，组队ID：{}，锁单商品：{} {} {}，组队商品：{} {} {}",
                    checkLockEntity.getUserId(), checkLockEntity.getTeamId(),
                    checkLockEntity.getGoodsId(), checkLockEntity.getSource(), checkLockEntity.getChannel(),
                    groupBuyTeamEntity.getGoodsId(), groupBuyTeamEntity.getSource(), groupBuyTeamEntity.getChannel());
            throw new AppException(ResponseCodeEnum.E0030.getCode(), ResponseCodeEnum.E0030.getInfo());
        }
        // 组队必须为拼单中且未过有效期
        if (!TeamStatusEnum.PROGRESS.equals(groupBuyTeamEntity.getTeamStatusEnum()) || new Date().after(groupBuyTeamEntity.getEndTime())) {
            throw new AppException(ResponseCodeEnum.E0029.getCode(), ResponseCodeEnum.E0029.getInfo());
        }

        return next(checkLockEntity, dynamicContext);
    }

}
