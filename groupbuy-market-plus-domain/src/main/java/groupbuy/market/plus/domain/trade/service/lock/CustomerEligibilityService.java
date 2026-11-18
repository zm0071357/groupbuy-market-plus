package groupbuy.market.plus.domain.trade.service.lock;

import groupbuy.market.plus.domain.trade.adapter.port.ShopOrderCountPort;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.OptionalLong;

/** 老带新资格，按历史有效下单次数区分开团和参团。 */
@Service
public class CustomerEligibilityService {

    private static final int NEW_CUSTOMER_MAX_ORDERS = 5;

    @Resource
    private ShopOrderCountPort shopOrderCountPort;

    /** 店铺资格及两种操作各自的拒绝原因。 */
    public enum Qualification {
        NEW(ResponseCodeEnum.E0047, null),
        RETURNING(null, ResponseCodeEnum.E0048),
        UNAVAILABLE(ResponseCodeEnum.E0049, ResponseCodeEnum.E0049);

        private final ResponseCodeEnum openRejection;
        private final ResponseCodeEnum joinRejection;

        Qualification(ResponseCodeEnum openRejection, ResponseCodeEnum joinRejection) {
            this.openRejection = openRejection;
            this.joinRejection = joinRejection;
        }

        /** 未通过时返回对应业务响应码。 */
        public ResponseCodeEnum rejection(boolean joining) {
            return joining ? joinRejection : openRejection;
        }
    }

    /** 商品页可省略当前订单号，锁单必须排除本次订单。 */
    public Qualification qualify(String shopId, String userId, String excludedOutTradeNo) {
        OptionalLong count = shopOrderCountPort.count(shopId, userId, excludedOutTradeNo);
        if (count.isEmpty() || count.getAsLong() < 0) {
            return Qualification.UNAVAILABLE;
        }
        return count.getAsLong() > NEW_CUSTOMER_MAX_ORDERS ? Qualification.RETURNING : Qualification.NEW;
    }
}
