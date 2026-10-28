package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.DiscountTypeEnum;
import groupbuy.market.plus.domain.activity.service.discount.DisCountService;
import groupbuy.market.plus.domain.activity.service.discount.impl.*;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * 优惠价格单元测试 - 保留两位小数（四舍五入）、最低 0.01 元、不高于原价
 */
public class DiscountPriceUnitTest {

    @Test
    public void test_zk_roundToCent() {
        // 99.99 × 0.85 = 84.9915 → 84.99
        assertPrice("84.99", new ZKServiceImpl(), "99.99", "0.85");
        // 10.01 × 0.5 = 5.005 → 5.01（四舍五入）
        assertPrice("5.01", new ZKServiceImpl(), "10.01", "0.5");
        // 0.01 × 0.1 = 0.001 → 最低 0.01
        assertPrice("0.01", new ZKServiceImpl(), "0.01", "0.1");
    }

    @Test
    public void test_mzk_roundToCent() {
        assertPrice("84.99", new MZKServiceImpl(), "99.99", "50,0.85");
        // 未达门槛返回原价
        assertPrice("49.99", new MZKServiceImpl(), "49.99", "50,0.85");
    }

    @Test
    public void test_zj_minPrice() {
        assertPrice("80.00", new ZJServiceImpl(), "100", "20");
        // 直减金额超过原价 → 最低 0.01
        assertPrice("0.01", new ZJServiceImpl(), "10", "20");
    }

    @Test
    public void test_mj_minPrice() {
        assertPrice("80.00", new MJServiceImpl(), "100", "100,20");
        assertPrice("0.01", new MJServiceImpl(), "100", "100,200");
    }

    @Test
    public void test_n_notAboveOriginal() {
        assertPrice("9.90", new NServiceImpl(), "100", "9.9");
        // N 元购价格高于原价 → 按原价，优惠金额不为负
        assertPrice("100", new NServiceImpl(), "100", "150");
    }

    @Test
    public void test_headerPrice_roundToCent() throws Exception {
        Method method = TradeRepositoryImpl.class.getDeclaredMethod("calculatePayPrice", Boolean.class, BigDecimal.class);
        method.setAccessible(true);
        TradeRepositoryImpl tradeRepository = new TradeRepositoryImpl();

        // 84.99 × 0.95 = 80.7405 → 80.74
        Assert.assertEquals(new BigDecimal("80.74"), method.invoke(tradeRepository, true, new BigDecimal("84.99")));
        // 团员不打折
        Assert.assertEquals(new BigDecimal("84.99"), method.invoke(tradeRepository, false, new BigDecimal("84.99")));
        // 0.01 × 0.95 → 最低 0.01
        Assert.assertEquals(0, new BigDecimal("0.01").compareTo((BigDecimal) method.invoke(tradeRepository, true, new BigDecimal("0.01"))));
    }

    private void assertPrice(String expected, DisCountService disCountService, String originalPrice, String marketExpr) {
        ActivityVO.GroupBuyDiscount discount = ActivityVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr(marketExpr)
                .build();
        BigDecimal price = disCountService.calculate("user01", new BigDecimal(originalPrice), discount);
        Assert.assertEquals("原价 " + originalPrice + "，表达式 " + marketExpr, 0, new BigDecimal(expected).compareTo(price));
        Assert.assertTrue("应保留不超过两位小数：" + price, price.scale() <= 2);
    }

}
