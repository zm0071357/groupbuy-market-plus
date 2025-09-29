package groupbuy.market.plus.domain.activity.service;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.entity.IndexTeamEntity;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class IndexGroupBuyMarketServiceImpl implements IndexGroupBuyMarketService{

    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;

    @Resource
    private ActivityRepository activityRepository;

    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();
        TrialBalanceEntity trialBalanceEntity = strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());
        log.info("用户ID：{}，商品ID：{}，试算结果：{}", marketProductEntity.getUserId(), marketProductEntity.getGoodsId(), JSON.toJSONString(trialBalanceEntity));
        return trialBalanceEntity;
    }

    @Override
    public List<IndexTeamEntity> getTeamList(Long activityId, String userId, String goodsId, String source, String channel, Integer userTeamCount, Integer randomTeamCount) {
        List<IndexTeamEntity> indexTeamEntityList = new ArrayList<>();
        List<IndexTeamEntity> userTeamEntityList = new ArrayList<>();
        if (userTeamCount != 0) {
            // 获取个人拼团组队
            userTeamEntityList = activityRepository.getUserTeam(activityId, userId, goodsId, source, channel, userTeamCount);
            if (userTeamEntityList != null && !userTeamEntityList.isEmpty()) {
                indexTeamEntityList.addAll(userTeamEntityList);
            }
        }
        // 个人没有拼团组队 - 随机拼团组队数量需加上个人的数量
        if (userTeamEntityList != null && userTeamEntityList.isEmpty()) {
            randomTeamCount += userTeamCount;
        }
        // 获取随机拼团组队
        List<IndexTeamEntity> randomTeamEntityList = activityRepository.getRandomTeam(activityId, userId, goodsId, source, channel, randomTeamCount);
        if (randomTeamEntityList != null && !randomTeamEntityList.isEmpty()) {
            indexTeamEntityList.addAll(randomTeamEntityList);
        }
        return indexTeamEntityList;
    }

    @Override
    public StatisticsVO getStatistics(Long activityId, String goodsId) {
        return activityRepository.getStatistics(activityId, goodsId);
    }

}
