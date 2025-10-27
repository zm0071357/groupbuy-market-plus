package groupbuy.market.plus.domain.trade.service.refund.thread;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundThreadTaskAggregate;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;

import java.util.concurrent.Callable;

/**
 * 异步任务 - 查询拼团信息
 */
public class GetAggregateThreadTask implements Callable<RefundThreadTaskAggregate> {

    private final TradeRepository tradeRepository;

    private final PreRefundEntity preRefundEntity;

    public GetAggregateThreadTask(PreRefundEntity preRefundEntity, TradeRepository tradeRepository) {
        this.preRefundEntity = preRefundEntity;
        this.tradeRepository = tradeRepository;
    }

    @Override
    public RefundThreadTaskAggregate call() throws Exception {
        return tradeRepository.getRefundThreadTaskResAggregate(preRefundEntity);
    }
}
