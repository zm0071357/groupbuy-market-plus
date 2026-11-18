package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 与一笔拼团订单绑定的免拼累计或消耗记录。 */
@Getter
@AllArgsConstructor
public class SkipGroupRecord {
    private final String orderId;
    private final String userId;
    private final String source;
    private final String channel;
    private final String state;

    /** 按资格操作读写同一用户账户。 */
    public SkipGroupAccount account() {
        SkipGroupAccount account = new SkipGroupAccount();
        account.setUserId(userId);
        account.setSource(source);
        account.setChannel(channel);
        return account;
    }
}
