package groupbuy.market.plus.test.domain.activity;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 规则树单元测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TreeTest {

    @Resource
    private IndexGroupBuyMarketService indexGroupBuyMarketService;

    @Test
    public void testTreeFramework() throws Exception {
        indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                        .userId("399547479")
                        .goodId("1001")
                        .channel("c01")
                        .source("s01")
                .build());
    }

}
