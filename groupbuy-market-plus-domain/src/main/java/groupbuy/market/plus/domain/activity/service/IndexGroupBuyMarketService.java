package groupbuy.market.plus.domain.activity.service;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;

public interface IndexGroupBuyMarketService {

    /**
     * 试算
     * @param marketProductEntity
     * @return
     * @throws Exception
     */
    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;

}
