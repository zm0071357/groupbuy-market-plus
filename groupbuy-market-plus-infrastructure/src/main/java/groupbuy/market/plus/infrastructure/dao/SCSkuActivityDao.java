package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SCSkuActivityDao {
    SCSkuActivity getSCSkuActivityByIdWithSC(SCSkuActivity scSkuActivityReq);
}
