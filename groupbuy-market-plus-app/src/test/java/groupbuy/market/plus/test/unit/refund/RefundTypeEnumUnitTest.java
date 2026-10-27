package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import org.junit.Assert;
import org.junit.Test;

/**
 * 退单类型匹配单元测试
 */
public class RefundTypeEnumUnitTest {

    @Test
    public void test_completeTeam_matchCompletePaidRefund() {
        Assert.assertEquals(RefundTypeEnum.TEAM_COMPLETE_PAID_REFUND,
                RefundTypeEnum.get(OrderStatusEnum.COMPLETE, TeamStatusEnum.COMPLETE));
    }

    @Test
    public void test_completeRefundTeam_matchCompletePaidRefund() {
        // 成团后已有成员退单（完成 - 存在退单），其他成员仍可退单
        Assert.assertEquals(RefundTypeEnum.TEAM_COMPLETE_PAID_REFUND,
                RefundTypeEnum.get(OrderStatusEnum.COMPLETE, TeamStatusEnum.COMPLETE_REFUND));
    }

    @Test
    public void test_progressTeam_unchanged() {
        Assert.assertEquals(RefundTypeEnum.TEAM_INCOMPLETE_UNPAID_REFUND,
                RefundTypeEnum.get(OrderStatusEnum.CREATE, TeamStatusEnum.PROGRESS));
        Assert.assertEquals(RefundTypeEnum.TEAM_INCOMPLETE_PAID_REFUND,
                RefundTypeEnum.get(OrderStatusEnum.COMPLETE, TeamStatusEnum.PROGRESS));
    }

}
