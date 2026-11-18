package groupbuy.market.plus.domain.activity.service.admin;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityAdminRepository;
import groupbuy.market.plus.domain.activity.model.entity.ManagedActivityEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyPrice;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.List;

/** 后台草稿、预览、发布和下架用例。 */
@Service
public class ActivityAdminService {

    @Resource
    private ActivityAdminRepository repository;

    /** 查询商品供运营选择。 */
    public List<ActivityAdminRepository.Product> products(long afterId, int limit) {
        validatePage(afterId, limit);
        return repository.products(afterId, limit);
    }

    /** 查询后台活动列表。 */
    public List<ManagedActivityEntity> list(long afterId, int limit) {
        validatePage(afterId, limit);
        return repository.list(afterId, limit);
    }

    /** 查询一条运营活动记录。 */
    public ManagedActivityEntity detail(long id) {
        ManagedActivityEntity activity = repository.find(id);
        if (activity == null) {
            throw failure(ResponseCodeEnum.E0039);
        }
        return activity;
    }

    /** 使用真实商品的原价校验并预览配置。 */
    public ActivityDraftVO preview(ActivityDraftVO draft) {
        if (draft == null || !text(draft.name(), 64) || !text(draft.goodsId(), 32)
                || !text(draft.source(), 16) || !text(draft.channel(), 16)
                || draft.eligibilityShopId() != null && (!text(draft.eligibilityShopId(), 64)
                     || draft.eligibilityShopId().codePoints().anyMatch(Character::isISOControl))
                || draft.startTime() == null || draft.endTime() == null || !draft.startTime().isBefore(draft.endTime())
                || draft.validMinutes() == null || draft.validMinutes() < 1 || draft.validMinutes() > 10080
                || draft.userLimit() == null || draft.userLimit() < 1 || draft.userLimit() > 10000
                || draft.tiers() == null || draft.tiers().isEmpty() || draft.tiers().size() > 10
                || draft.tiers().stream().anyMatch(tier -> tier != null && tier.targetCount() != null && tier.targetCount() > 10000)) {
            throw failure(ResponseCodeEnum.ILLEGAL_PARAMETER);
        }
        ActivityAdminRepository.Product product = repository.product(draft);
        if (product == null) {
            throw failure(ResponseCodeEnum.E0040);
        }
        boolean skipGroupEnabled = Boolean.TRUE.equals(draft.skipGroupEnabled());
        if (skipGroupEnabled && draft.eligibilityShopId() != null) {
            throw failure(ResponseCodeEnum.E0050);
        }
        if (skipGroupEnabled && !GroupBuyPrice.isValidOffer(draft.skipGroupPrice(), product.originalPrice())
                || !skipGroupEnabled && draft.skipGroupPrice() != null) {
            throw failure(ResponseCodeEnum.E0044);
        }
        return new ActivityDraftVO(draft.name().strip(), draft.goodsId(), draft.source(), draft.channel(),
                draft.startTime(), draft.endTime(), draft.validMinutes(), draft.userLimit(),
                GroupBuyTierVO.validate(draft.tiers(), product.originalPrice()), skipGroupEnabled,
                skipGroupEnabled ? draft.skipGroupPrice().setScale(2) : null, draft.eligibilityShopId());
    }

    /** 保存新草稿。 */
    public long create(ActivityDraftVO draft) {
        return repository.create(preview(draft));
    }

    /** 保存草稿配置，发布后的活动配置保持稳定。 */
    public void update(long id, ActivityDraftVO draft) {
        if (!repository.update(id, preview(draft))) {
            throw failure(ResponseCodeEnum.E0041);
        }
    }

    /** 在事务中创建活动和商品映射，开始时间到达后可开团。 */
    public long publish(long id) {
        ManagedActivityEntity activity = detail(id);
        if (activity.state() == ManagedActivityEntity.State.PUBLISHED) {
            return activity.activityId();
        }
        if (activity.state() != ManagedActivityEntity.State.DRAFT || !activity.draft().endTime().isAfter(Instant.now())) {
            throw failure(ResponseCodeEnum.E0041);
        }
        preview(activity.draft());
        return repository.publish(id, activity.draft());
    }

    /** 下架已发布活动。 */
    public void offline(long id) {
        if (!repository.offline(id)) {
            throw failure(ResponseCodeEnum.E0041);
        }
    }

    private void validatePage(long afterId, int limit) {
        if (afterId < 0 || limit < 1 || limit > 100) {
            throw failure(ResponseCodeEnum.ILLEGAL_PARAMETER);
        }
    }

    private boolean text(String text, int limit) {
        return text != null && !text.isBlank() && text.length() <= limit && text.equals(text.strip());
    }

    private AppException failure(ResponseCodeEnum code) {
        return new AppException(code.getCode(), code.getInfo());
    }
}
