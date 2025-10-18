package groupbuy.market.plus.test.domain.trade;

import groupbuy.market.plus.api.dto.SettleOrderRequestDTO;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 退单服务单元测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RefundOrderServiceTest {

    @Resource
    private RefundOrderService refundOrderService;

    @Test
    public void test_refundOrder() throws Exception {
        PreRefundEntity preRefundEntity = new PreRefundEntity();
        preRefundEntity.setUserId("134137257");
        preRefundEntity.setOutTradeNo("QDLL640829482850");
        refundOrderService.refundOrder(preRefundEntity);
    }

}
