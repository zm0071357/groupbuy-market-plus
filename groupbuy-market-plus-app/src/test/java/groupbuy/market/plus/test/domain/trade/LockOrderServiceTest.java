package groupbuy.market.plus.test.domain.trade;

import groupbuy.market.plus.api.TradeService;
import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 锁单服务单元测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class LockOrderServiceTest {

    @Resource
    private TradeService tradeService;

    @Test
    public void test_lockOrder() {
        LockOrderRequestDTO lockOrderRequestDTO = new LockOrderRequestDTO();
        lockOrderRequestDTO.setUserId("137158130");
        lockOrderRequestDTO.setTeamId("TEAM34559911");
        lockOrderRequestDTO.setActivityId(100123L);
        lockOrderRequestDTO.setGoodsId("9890001");
        lockOrderRequestDTO.setSource("s01");
        lockOrderRequestDTO.setChannel("c01");
        lockOrderRequestDTO.setOutTradeNo("LTZF".concat(RandomStringUtils.randomNumeric(12)));
        tradeService.lockOrder(lockOrderRequestDTO);
    }


}
