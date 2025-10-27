package groupbuy.market.plus.domain.trade.service.refund;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundNotifyEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.strategy.RefundService;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RefundOrderServiceImpl implements RefundOrderService {

    @Resource
    private DefaultRefundStrategyFactory defaultRefundStrategyFactory;

    @Resource
    private Map<String, RefundService> refundServiceMap;

    @Resource
    private TradeRepository tradeRepository;

    @Override
    public RefundResEntity refundOrder(PreRefundEntity preRefundEntity) throws Exception {
        StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> strategyHandler = defaultRefundStrategyFactory.strategyHandler();
        RefundResEntity refundResEntity = strategyHandler.apply(preRefundEntity, new DefaultRefundStrategyFactory.DynamicContext());
        log.info("用户ID：{}，外部交易单号：{}，退单结果：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo(), JSON.toJSONString(refundResEntity));
        return refundResEntity;
    }

    @Override
    public void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception {
        // 根据退单类型获取对应退单服务
        RefundTypeEnum refundTypeEnum = RefundTypeEnum.valueOf(orderRefundMessage.getType());
        RefundService refundService = refundServiceMap.get(refundTypeEnum.getStrategy());
        // 恢复拼团组队库存
        refundService.recoverTeamLockStock(orderRefundMessage);
    }

    @Override
    public Map<String, Integer> teamTimeoutRefund() throws Exception {
        // 获取超时的拼团ID集合
        List<String> timeoutTeamIdList = tradeRepository.getTimeoutTeamIdList();
        // 获取超时的拼团订单集合
        List<PreRefundEntity> preRefundEntityList = new ArrayList<>();
        if (timeoutTeamIdList != null && !timeoutTeamIdList.isEmpty()) {
            preRefundEntityList = tradeRepository.getTimeoutOrderList(timeoutTeamIdList);
        }

        Integer successCount = 0;
        Integer repeatCount = 0;
        Integer failCount = 0;

        if (!preRefundEntityList.isEmpty()) {
            // 遍历订单集合进行退单
            for (PreRefundEntity preRefundEntity : preRefundEntityList) {
                RefundResEntity refundResEntity = this.refundOrder(preRefundEntity);
                if (refundResEntity.getRefundStatusEnum().equals(RefundStatusEnum.SUCCESS)) {
                    successCount ++;
                } else if (refundResEntity.getRefundStatusEnum().equals(RefundStatusEnum.REPEAT)) {
                    repeatCount ++;
                } else {
                    failCount ++;
                }
            }
        }

        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("size", preRefundEntityList.size());
        resultMap.put("successCount", successCount);
        resultMap.put("repeatCount", repeatCount);
        resultMap.put("failCount", failCount);

        return resultMap;
    }

    @Override
    public TeamProgressVO getTeamProgress(String teamId) {
        return tradeRepository.getTeamProgress(teamId);
    }


}
