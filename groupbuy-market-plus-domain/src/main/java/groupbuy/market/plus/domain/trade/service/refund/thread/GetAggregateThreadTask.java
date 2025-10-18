package groupbuy.market.plus.domain.trade.service.refund.thread;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundThreadTaskAggregate;

import java.util.concurrent.Callable;

/**
 * 异步任务 - 查询拼团信息
 */
public class GetAggregateThreadTask implements Callable<RefundThreadTaskAggregate> {

    private final String userId;

    private final String outTradeNo;

    private final TradeRepository tradeRepository;

    public GetAggregateThreadTask(String userId, String outTradeNo, TradeRepository tradeRepository) {
        this.userId = userId;
        this.outTradeNo = outTradeNo;
        this.tradeRepository = tradeRepository;
    }

    @Override
    public RefundThreadTaskAggregate call() throws Exception {
        return tradeRepository.getRefundThreadTaskResAggregate(userId, outTradeNo);
    }
}
