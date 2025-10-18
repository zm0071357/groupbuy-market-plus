package groupbuy.market.plus.domain.trade.service.refund.strategy;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 退单抽象类
 */
@Slf4j
@Service
public abstract class AbstractRefundService implements RefundService {

    @Resource
    protected TradeRepository tradeRepository;

    @Override
    public void newHeaderNotify(String newLeaderUserId) {
        // TODO：使用MQ回调给外部系统
        log.info("新团长ID回调模拟：{}", newLeaderUserId);
    }
}
