package groupbuy.market.plus.domain.activity.model.entity;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;

/** 后台管理的活动及其发布状态。 */
public record ManagedActivityEntity(Long id, Long activityId, State state, ActivityDraftVO draft) {

    /** 运营活动生命周期。 */
    public enum State { DRAFT, PUBLISHED, OFFLINE }
}
