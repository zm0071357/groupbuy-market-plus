package groupbuy.market.plus.domain.activity.adapter.repository;

import groupbuy.market.plus.domain.activity.model.entity.ManagedActivityEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;

import java.math.BigDecimal;
import java.util.List;

/** 运营活动的查询与原子状态变更。 */
public interface ActivityAdminRepository {

    /** 后台可选择的商品及其来源渠道。 */
    record Product(Long id, String goodsId, String name, String source, String channel, BigDecimal originalPrice) {}

    /** 按商品主键正序查询商品。 */
    List<Product> products(long afterId, int limit);

    /** 获取指定来源渠道的商品。 */
    Product product(ActivityDraftVO draft);

    /** 按草稿主键正序查询管理记录。 */
    List<ManagedActivityEntity> list(long afterId, int limit);

    /** 获取一条管理记录。 */
    ManagedActivityEntity find(long id);

    /** 新建可编辑的活动草稿。 */
    long create(ActivityDraftVO draft);

    /** 更新尚未发布的草稿。 */
    boolean update(long id, ActivityDraftVO draft);

    /** 发布当前草稿，若被修改或商品占用则拒绝。 */
    long publish(long id, ActivityDraftVO expectedDraft);

    /** 停止新开团，保留已有组队所用的活动记录。 */
    boolean offline(long id);
}
