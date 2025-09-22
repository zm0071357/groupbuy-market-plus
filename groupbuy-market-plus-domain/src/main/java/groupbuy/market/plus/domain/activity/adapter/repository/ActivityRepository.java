package groupbuy.market.plus.domain.activity.adapter.repository;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SCSkuActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;

public interface ActivityRepository {

    /**
     * 根据商品ID获取拼团商品
     * @param goodsId
     * @return
     */
    SkuVO getSkuByGoodsId(String goodsId);

    /**
     * 根据活动ID获取活动
     * @param activityId
     * @return
     */
    ActivityVO getActivityById(Long activityId);

    /**
     * 查询商品活动关联
     * @param goodsId
     * @param source
     * @param channel
     * @return
     */
    SCSkuActivityVO getSCSkuActivityByGoodsIdWithSC(String goodsId, String source, String channel);
}
