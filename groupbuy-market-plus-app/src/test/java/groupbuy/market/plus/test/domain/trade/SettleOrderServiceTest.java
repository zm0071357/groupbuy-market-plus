package groupbuy.market.plus.test.domain.trade;

import groupbuy.market.plus.api.TradeService;
import groupbuy.market.plus.api.dto.SettleOrderRequestDTO;
import groupbuy.market.plus.domain.trade.model.entity.OrderPaySuccessEntity;
import groupbuy.market.plus.domain.trade.service.settle.SettleOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 结算服务单元测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SettleOrderServiceTest {

    @Resource
    private TradeService tradeService;

    @Test
    public void test_settleOrder() throws Exception {
        SettleOrderRequestDTO settleOrderRequestDTO = new SettleOrderRequestDTO();
        settleOrderRequestDTO.setUserId("399547479");
        settleOrderRequestDTO.setOutTradeNo("LTZF123456789123");
        settleOrderRequestDTO.setOutTradeNoPayTime(new Date());
        settleOrderRequestDTO.setSource("s01");
        settleOrderRequestDTO.setChannel("c01");
        tradeService.settleOrder(settleOrderRequestDTO);
    }

}
