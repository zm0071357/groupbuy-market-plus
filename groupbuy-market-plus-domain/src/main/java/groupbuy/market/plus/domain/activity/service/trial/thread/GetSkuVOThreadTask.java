package groupbuy.market.plus.domain.activity.service.trial.thread;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;

import java.util.concurrent.Callable;

/**
 * 异步任务 - 查询拼团商品
 */
public class GetSkuVOThreadTask implements Callable<SkuVO> {

    private final String goodsId;

    private final ActivityRepository activityRepository;

    public GetSkuVOThreadTask(String goodsId, ActivityRepository activityRepository) {
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    @Override
    public SkuVO call() throws Exception {
        return activityRepository.getSkuByGoodsId(goodsId);
    }
}
