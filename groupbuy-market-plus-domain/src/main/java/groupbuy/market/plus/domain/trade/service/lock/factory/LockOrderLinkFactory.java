package groupbuy.market.plus.domain.trade.service.lock.factory;

import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockEntity;
import groupbuy.market.plus.domain.trade.model.entity.CheckLockResEntity;
import groupbuy.market.plus.domain.trade.service.lock.filter.ActivityFilter;
import groupbuy.market.plus.domain.trade.service.lock.filter.UserFilter;
import groupbuy.market.plus.types.design.framework.link.multition.LinkArmory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 锁单责任链工厂
 */
@Service
public class LockOrderLinkFactory {

    @Bean("lockOrderLink")
    public BusinessLinkedList<CheckLockEntity, DynamicContext, CheckLockResEntity> lockOrderLink(ActivityFilter activityFilter, UserFilter userFilter) {
        // 组装链
        LinkArmory<CheckLockEntity, DynamicContext, CheckLockResEntity> linkArmory = new LinkArmory<>("锁单责任链", activityFilter, userFilter);
        return linkArmory.getLogicLink();
    }

    /**
     * 动态上下文信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /**
         * 活动实体
         */
        private ActivityEntity activityEntity;
    }
}
