package groupbuy.market.plus.domain.activity.service.trial.node;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.discount.DisCountService;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetActivityVOThreadTask;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetSkuVOThreadTask;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 试算节点
 */
@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private EndNode endNode;

    @Resource
    private ErrorNode errorNode;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private Map<String, DisCountService> disCountServiceMap;

    @Override
    protected void multiThread(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询拼团商品
        GetSkuVOThreadTask getSkuVOThreadTask = new GetSkuVOThreadTask(marketProductEntity.getGoodsId(), activityRepository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(getSkuVOThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        // 异步查询拼团商品
        GetActivityVOThreadTask getActivityVOThreadTask = new GetActivityVOThreadTask(marketProductEntity.getGoodsId(), marketProductEntity.getSource(), marketProductEntity.getChannel(), activityRepository);
        FutureTask<ActivityVO> activityVOFutureTask = new FutureTask<>(getActivityVOThreadTask);
        threadPoolExecutor.execute(activityVOFutureTask);

        // 写入上下文
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setActivityVO(activityVOFutureTask.get(timeout, TimeUnit.MINUTES));

        log.info("用户ID：{}，异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", marketProductEntity.getUserId());
    }

    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品试算服务-试算节点 userId：{}", marketProductEntity.getUserId());
        ActivityVO activityVO = dynamicContext.getActivityVO();
        if (activityVO == null) {
            return router(marketProductEntity, dynamicContext);
        }
        SkuVO skuVO = dynamicContext.getSkuVO();
        List<ActivityVO.GroupBuyDiscount> groupBuyDiscountList = activityVO.getGroupBuyDiscountList();
        if (skuVO == null || (groupBuyDiscountList == null || groupBuyDiscountList.isEmpty())) {
            return router(marketProductEntity, dynamicContext);
        }
        log.info("商品信息：{}", JSON.toJSONString(skuVO));
        log.info("活动信息：{}", JSON.toJSONString(activityVO));
        log.info("折扣信息：{}", JSON.toJSONString(groupBuyDiscountList));
        BigDecimal price = skuVO.getOriginalPrice();
        for (ActivityVO.GroupBuyDiscount groupBuyDiscount : groupBuyDiscountList) {
            DisCountService disCountService = disCountServiceMap.get(groupBuyDiscount.getMarketPlan());
            if (disCountService == null) {
                throw new AppException(ResponseCodeEnum.E0001.getCode(), ResponseCodeEnum.E0001.getInfo());
            }
            price = disCountService.calculate(marketProductEntity.getUserId(), price, groupBuyDiscount);
        }
        dynamicContext.setDeductionPrice(skuVO.getOriginalPrice().subtract(price));
        dynamicContext.setPayPrice(price);
        return router(marketProductEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        if (dynamicContext.getActivityVO() == null ||
                dynamicContext.getSkuVO() == null ||
                dynamicContext.getPayPrice() == null) {
            return errorNode;
        }
        return endNode;
    }

}
