package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.ManagedActivity;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import groupbuy.market.plus.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 后台草稿和商品发布持久化接口。 */
@Mapper
public interface ActivityAdminDao {
    /** 查询一页可供配置的商品。 */
    List<Sku> products(@Param("afterId") long afterId, @Param("limit") int limit);

    /** 查询目标商品，发布时锁定该商品以串行处理其活动映射。 */
    Sku product(@Param("goodsId") String goodsId, @Param("source") String source,
                @Param("channel") String channel, @Param("lock") boolean lock);

    /** 查询一页后台活动。 */
    List<ManagedActivity> list(@Param("afterId") long afterId, @Param("limit") int limit);

    /** 查找草稿，变更状态时锁行。 */
    ManagedActivity find(@Param("id") long id, @Param("lock") boolean lock);

    /** 创建草稿并回填主键。 */
    void create(ManagedActivity activity);

    /** 更新草稿，发布后拒绝修改。 */
    int updateDraft(ManagedActivity activity);

    /** 更新已锁定的管理记录状态。 */
    int changeState(ManagedActivity activity);

    /** 仅释放无在途组队且已下架、结束或失效的原商品映射。 */
    int replaceInactiveMapping(SCSkuActivity mapping);

    /** 为尚无活动的商品创建映射。 */
    void insertMapping(SCSkuActivity mapping);
}
