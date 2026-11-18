package groupbuy.market.plus.infrastructure.adapter.repository;

import groupbuy.market.plus.domain.trade.model.valobj.SkipGroupBalanceVO;
import groupbuy.market.plus.infrastructure.dao.SkipGroupDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.SkipGroupAccount;
import groupbuy.market.plus.infrastructure.dao.po.SkipGroupRecord;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

/** 免拼记录随拼团订单写入同一个数据库事务。 */
@Component
public class SkipGroupLedger {
    private static final String EARNED = "EARNED";
    private static final String REVOKED = "REVOKED";
    private static final String HELD = "HELD";
    private static final String USED = "USED";
    private static final String RELEASED = "RELEASED";

    @Resource
    private SkipGroupDao dao;

    /** 查询用户当前可用次数和下一次机会尚需的有效订单数。 */
    public SkipGroupBalanceVO balance(String userId, String source, String channel) {
        SkipGroupAccount account = dao.getAccount(new SkipGroupRecord(null, userId, source, channel, null).account());
        return account == null ? SkipGroupBalanceVO.from(0, 0, 0)
                : SkipGroupBalanceVO.from(account.getEarnedCount(), account.getHeldCount(), account.getUsedCount());
    }

    /** 锁单时原子占用一次机会。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(String orderId, String userId, String source, String channel) {
        SkipGroupRecord record = new SkipGroupRecord(orderId, userId, source, channel, HELD);
        SkipGroupAccount account = record.account();
        dao.ensureAccount(account);
        if (dao.reserve(account) != 1) {
            throw failure(ResponseCodeEnum.E0045);
        }
        dao.insertRecord(record);
    }

    /** 普通团或档位团成团后，每笔已支付订单贡献一次累计。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void earn(List<GroupBuyTeamOrder> orders) {
        // 同时涉及多个账户时统一加锁顺序，降低不同组队同时成团的死锁概率。
        orders.stream().sorted(Comparator.comparing(GroupBuyTeamOrder::getUserId, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GroupBuyTeamOrder::getSource, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GroupBuyTeamOrder::getChannel, String.CASE_INSENSITIVE_ORDER))
                .forEach(order -> {
                    SkipGroupRecord record = new SkipGroupRecord(order.getOrderId(), order.getUserId(),
                            order.getSource(), order.getChannel(), EARNED);
                    SkipGroupAccount account = record.account();
                    dao.ensureAccount(account);
                    dao.insertRecord(record);
                    adjust(account, 1, 0, 0);
                });
    }

    /** 免拼支付成功后正式扣除。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(String orderId) {
        transition(dao.getRecordForUpdate(orderId), HELD, USED, 0, -1, 1);
    }

    /** 免拼未支付退单释放占用。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(String orderId) {
        transition(dao.getRecordForUpdate(orderId), HELD, RELEASED, 0, -1, 0);
    }

    /** 已贡献累计的成团订单退单时扣回进度，历史无记录的订单和免拼订单无变化。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeEarning(String orderId) {
        SkipGroupRecord record = dao.getRecordForUpdate(orderId);
        if (record != null && EARNED.equals(record.getState())) {
            transition(record, EARNED, REVOKED, -1, 0, 0);
        }
    }

    private void transition(SkipGroupRecord record, String from, String to, long earned, long held, long used) {
        if (record == null || dao.updateRecord(record.getOrderId(), from, to) != 1) {
            throw failure(ResponseCodeEnum.UPDATE_ZERO);
        }
        adjust(record.account(), earned, held, used);
    }

    private void adjust(SkipGroupAccount account, long earned, long held, long used) {
        account.setEarnedCount(earned);
        account.setHeldCount(held);
        account.setUsedCount(used);
        if (dao.updateCounts(account) != 1) {
            throw failure(ResponseCodeEnum.UPDATE_ZERO);
        }
    }

    private AppException failure(ResponseCodeEnum code) {
        return new AppException(code.getCode(), code.getInfo());
    }
}
