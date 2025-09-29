package groupbuy.market.plus.domain.activity.service;

import groupbuy.market.plus.domain.activity.model.entity.IndexTeamEntity;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;

import java.util.List;

public interface IndexGroupBuyMarketService {

    /**
     * 试算
     * @param marketProductEntity
     * @return
     * @throws Exception
     */
    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;

    /**
     * 查询拼团组队信息
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param goodsId 商品ID
     * @param source 来源
     * @param channel 渠道
     * @param userTeamCount 用户参与的拼团组队数量
     * @param randomTeamCount 随机的拼团组队数量
     * @return
     */
    List<IndexTeamEntity> getTeamList(Long activityId, String userId, String goodsId, String source,
                                      String channel, Integer userTeamCount, Integer randomTeamCount);

    /**
     * 查询统计信息
     * @param activityId 活动ID
     * @param goodsId 商品ID
     * @return
     */
    StatisticsVO getStatistics(Long activityId, String goodsId);
}
