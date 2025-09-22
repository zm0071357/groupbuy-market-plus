package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.Discount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiscountDao {
    Discount getDiscountById(String discountId);
}
