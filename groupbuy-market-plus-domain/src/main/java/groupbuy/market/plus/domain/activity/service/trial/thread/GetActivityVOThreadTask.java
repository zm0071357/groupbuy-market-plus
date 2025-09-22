package groupbuy.market.plus.domain.activity.service.trial.thread;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SCSkuActivityVO;

import java.util.concurrent.Callable;

/**
 * 异步任务 - 查询活动配置
 */
public class GetActivityVOThreadTask implements Callable<ActivityVO> {

    private final String goodId;

    private final String source;

    private final String channel;

    private final ActivityRepository activityRepository;

    public GetActivityVOThreadTask(String goodId, String source, String channel, ActivityRepository activityRepository) {
        this.goodId = goodId;
        this.source = source;
        this.channel = channel;
        this.activityRepository = activityRepository;
    }

    @Override
    public ActivityVO call() throws Exception {
        SCSkuActivityVO scSkuActivityVO = activityRepository.getSCSkuActivityByGoodsIdWithSC(goodId, source, channel);
        if (scSkuActivityVO == null) {
            return null;
        }
        return activityRepository.getActivityById(scSkuActivityVO.getActivityId());
    }
}
