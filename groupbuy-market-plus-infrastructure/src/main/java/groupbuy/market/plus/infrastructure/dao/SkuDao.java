package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SkuDao {

    Sku getSkuByGoodsId(String goodsId);
}
