package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.SkipGroupAccount;
import groupbuy.market.plus.infrastructure.dao.po.SkipGroupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 同一账户的时间及次数变更由数据库串行处理。 */
@Mapper
public interface SkipGroupDao {
    /** 读取用户免拼账户。 */
    SkipGroupAccount getAccount(SkipGroupAccount scope);

    /** 创建账户或锁定已有账户。 */
    void ensureAccount(SkipGroupAccount scope);

    /** 余额充足时占用一次。 */
    int reserve(SkipGroupAccount scope);

    /** 更新同一账户计数。 */
    int updateCounts(SkipGroupAccount scope);

    /** 查询订单累计或占用记录。 */
    SkipGroupRecord getRecordForUpdate(String orderId);

    /** 插入一笔累计或占用记录。 */
    void insertRecord(SkipGroupRecord record);

    /** 按原状态更新记录，避免重复扣除或归还。 */
    int updateRecord(@Param("orderId") String orderId, @Param("from") String from, @Param("to") String to);
}
