package groupbuy.market.plus.infrastructure.adapter.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import groupbuy.market.plus.domain.activity.adapter.repository.ActivityAdminRepository;
import groupbuy.market.plus.domain.activity.model.entity.ManagedActivityEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyPrice;
import groupbuy.market.plus.infrastructure.dao.ActivityAdminDao;
import groupbuy.market.plus.infrastructure.dao.ActivityDao;
import groupbuy.market.plus.infrastructure.dao.SCSkuActivityDao;
import groupbuy.market.plus.infrastructure.dao.po.Activity;
import groupbuy.market.plus.infrastructure.dao.po.ManagedActivity;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import groupbuy.market.plus.infrastructure.dao.po.Sku;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** 运营管理持久化，活动与路由在同一个事务中生效。 */
@Repository
public class ActivityAdminRepositoryImpl implements ActivityAdminRepository {

    @Resource
    private ActivityAdminDao adminDao;
    @Resource
    private ActivityDao activityDao;
    @Resource
    private SCSkuActivityDao scSkuActivityDao;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<Product> products(long afterId, int limit) {
        return adminDao.products(afterId, limit).stream().map(this::toProduct).toList();
    }

    @Override
    public Product product(ActivityDraftVO draft) {
        Sku sku = adminDao.product(draft.goodsId(), draft.source(), draft.channel(), false);
        return sku == null ? null : toProduct(sku);
    }

    @Override
    public List<ManagedActivityEntity> list(long afterId, int limit) {
        return adminDao.list(afterId, limit).stream().map(this::toEntity).toList();
    }

    @Override
    public ManagedActivityEntity find(long id) {
        ManagedActivity row = adminDao.find(id, false);
        return row == null ? null : toEntity(row);
    }

    @Override
    public long create(ActivityDraftVO draft) {
        ManagedActivity row = new ManagedActivity();
        row.setDraftJson(encode(draft));
        adminDao.create(row);
        return row.getId();
    }

    @Override
    public boolean update(long id, ActivityDraftVO draft) {
        ManagedActivity row = new ManagedActivity();
        row.setId(id);
        row.setDraftJson(encode(draft));
        return adminDao.updateDraft(row) == 1;
    }

    @Override
    @Transactional
    public long publish(long id, ActivityDraftVO expectedDraft) {
        ManagedActivity row = adminDao.find(id, true);
        if (row == null) {
            throw failure(ResponseCodeEnum.E0039);
        }
        ManagedActivityEntity current = toEntity(row);
        if (current.state() == ManagedActivityEntity.State.PUBLISHED) {
            return current.activityId();
        }
        if (current.state() != ManagedActivityEntity.State.DRAFT || !expectedDraft.equals(current.draft())
                || !expectedDraft.endTime().isAfter(Instant.now())) {
            throw failure(ResponseCodeEnum.E0041);
        }
        Sku sku = adminDao.product(expectedDraft.goodsId(), expectedDraft.source(), expectedDraft.channel(), true);
        if (sku == null) {
            throw failure(ResponseCodeEnum.E0040);
        }
        List<GroupBuyTierVO> tiers = GroupBuyTierVO.validate(expectedDraft.tiers(), sku.getOriginalPrice());
        if (Boolean.TRUE.equals(expectedDraft.skipGroupEnabled())
                && !GroupBuyPrice.isValidOffer(expectedDraft.skipGroupPrice(), sku.getOriginalPrice())) {
            throw failure(ResponseCodeEnum.E0044);
        }
        SCSkuActivity mapping = SCSkuActivity.builder().activityId(id).goodsId(sku.getGoodsId())
                .source(sku.getSource()).channel(sku.getChannel()).tiersJson(encode(tiers))
                .skipGroupPrice(Boolean.TRUE.equals(expectedDraft.skipGroupEnabled()) ? expectedDraft.skipGroupPrice() : null).build();

        try {
            activityDao.insertManaged(Activity.builder().activityId(id).activityName(expectedDraft.name())
                    .takeLimitCount(expectedDraft.userLimit()).validTime(expectedDraft.validMinutes())
                    .target(tiers.get(0).targetCount()).startTime(Date.from(expectedDraft.startTime()))
                    .endTime(Date.from(expectedDraft.endTime())).build());
            if (scSkuActivityDao.getSCSkuActivityByIdWithSC(mapping) == null) {
                adminDao.insertMapping(mapping);
            } else if (adminDao.replaceInactiveMapping(mapping) != 1) {
                throw failure(ResponseCodeEnum.E0042);
            }
        } catch (DuplicateKeyException e) {
            throw failure(ResponseCodeEnum.E0042);
        }
        row.setActivityId(id);
        row.setState(ManagedActivityEntity.State.PUBLISHED.name());
        if (adminDao.changeState(row) != 1) {
            throw failure(ResponseCodeEnum.E0041);
        }
        return id;
    }

    @Override
    @Transactional
    public boolean offline(long id) {
        ManagedActivity row = adminDao.find(id, true);
        if (row == null) {
            throw failure(ResponseCodeEnum.E0039);
        }
        if (ManagedActivityEntity.State.OFFLINE.name().equals(row.getState())) {
            return true;
        }
        if (!ManagedActivityEntity.State.PUBLISHED.name().equals(row.getState())) {
            return false;
        }
        if (activityDao.stopManagedOpening(row.getActivityId()) != 1) {
            throw failure(ResponseCodeEnum.E0041);
        }
        row.setState(ManagedActivityEntity.State.OFFLINE.name());
        if (adminDao.changeState(row) != 1) {
            throw failure(ResponseCodeEnum.E0041);
        }
        return true;
    }

    private Product toProduct(Sku sku) {
        return new Product(sku.getId(), sku.getGoodsId(), sku.getGoodsName(), sku.getSource(), sku.getChannel(),
                sku.getOriginalPrice());
    }

    private ManagedActivityEntity toEntity(ManagedActivity row) {
        try {
            return new ManagedActivityEntity(row.getId(), row.getActivityId(), ManagedActivityEntity.State.valueOf(row.getState()),
                    objectMapper.readValue(row.getDraftJson(), ActivityDraftVO.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法读取活动草稿 " + row.getId(), e);
        }
    }

    private String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法保存活动配置", e);
        }
    }

    private AppException failure(ResponseCodeEnum code) {
        return new AppException(code.getCode(), code.getInfo());
    }
}
