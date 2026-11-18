package groupbuy.market.plus.infrastructure.dao.po;

import lombok.Data;

/** 免拼机会账户及计数。 */
@Data
public class SkipGroupAccount {
    private String userId;
    private String source;
    private String channel;
    private long earnedCount;
    private long heldCount;
    private long usedCount;
}
