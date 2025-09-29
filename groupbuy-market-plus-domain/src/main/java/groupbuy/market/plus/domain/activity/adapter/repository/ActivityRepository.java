package groupbuy.market.plus.domain.activity.adapter.repository;

import groupbuy.market.plus.domain.activity.model.entity.IndexTeamEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SCSkuActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;

import java.util.List;

public interface ActivityRepository {

    /**
     * 根据商品ID获取拼团商品
     * @param goodsId 商品ID
     * @return
     */
    SkuVO getSkuByGoodsId(String goodsId);

    /**
     * 根据活动ID获取活动
     * @param activityId 活动ID
     * @return
     */
    ActivityVO getActivityById(Long activityId);

    /**
     * 查询商品活动关联
     * @param goodsId 商品ID
     * @param source 来源
     * @param channel 渠道
     * @return
     */
    SCSkuActivityVO getSCSkuActivityByGoodsIdWithSC(String goodsId, String source, String channel);

    /**
     * 降级拦截
     * @return
     */
    boolean downgradeSwitch();

    /**
     * 切量拦截
     * @param userId 用户ID
     * @return
     */
    boolean cutRange(String userId);

    /**
     * 获取个人拼团组队
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @param source 来源
     * @param channel 渠道
     * @param userTeamCount 数量
     * @return
     */
    List<IndexTeamEntity> getUserTeam(Long activityId, String userId, String goodsId, String source, String channel, Integer userTeamCount);

    /**
     * 获取随机拼团组队
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @param source 来源
     * @param channel 渠道
     * @param randomTeamCount 数量
     * @return
     */
    List<IndexTeamEntity> getRandomTeam(Long activityId, String userId, String goodsId, String source, String channel, Integer randomTeamCount);

    /**
     * 查询统计信息
     * @param activityId 活动ID
     * @param goodsId 商品ID
     * @return
     */
    StatisticsVO getStatistics(Long activityId, String goodsId);

}
