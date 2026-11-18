package groupbuy.market.plus.trigger.http;

import groupbuy.market.plus.api.dto.ActivityAdminDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.ManagedActivityEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyPrice;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.service.admin.ActivityAdminService;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/** 提供独立运营后台所需的商品、草稿和发布接口。 */
@RestController
@RequestMapping("/admin/group-buy")
public class ActivityAdminController {

    @Resource
    private ActivityAdminService service;

    /** 按商品主键分页列出可选商品。 */
    @GetMapping("/products")
    public Response<List<ActivityAdminDTO.Product>> products(@RequestParam(defaultValue = "0") long afterId,
                                                            @RequestParam(defaultValue = "20") int limit) {
        return success(service.products(afterId, limit).stream().map(product -> new ActivityAdminDTO.Product(
                product.id(), product.goodsId(), product.name(), product.source(), product.channel(), product.originalPrice())).toList());
    }

    /** 按管理记录主键分页列出活动。 */
    @GetMapping("/activities")
    public Response<List<ActivityAdminDTO.Activity>> activities(@RequestParam(defaultValue = "0") long afterId,
                                                               @RequestParam(defaultValue = "20") int limit) {
        return success(service.list(afterId, limit).stream().map(this::toDto).toList());
    }

    /** 查询活动配置与发布状态。 */
    @GetMapping("/activities/{id}")
    public Response<ActivityAdminDTO.Activity> detail(@PathVariable long id) {
        return success(toDto(service.detail(id)));
    }

    /** 校验草稿并预览价格。 */
    @PostMapping("/activities/preview")
    public Response<ActivityAdminDTO.Draft> preview(@RequestBody ActivityAdminDTO.Draft draft) {
        return success(toDto(service.preview(toDomain(draft))));
    }

    /** 保存新草稿并返回草稿编号。 */
    @PostMapping("/activities")
    public Response<ActivityAdminDTO.Id> create(@RequestBody ActivityAdminDTO.Draft draft) {
        return success(new ActivityAdminDTO.Id(service.create(toDomain(draft))));
    }

    /** 更新尚未发布的草稿。 */
    @PutMapping("/activities/{id}")
    public Response<Void> update(@PathVariable long id, @RequestBody ActivityAdminDTO.Draft draft) {
        service.update(id, toDomain(draft));
        return success(null);
    }

    /** 发布草稿并返回交易侧活动编号。 */
    @PostMapping("/activities/{id}/publish")
    public Response<ActivityAdminDTO.Id> publish(@PathVariable long id) {
        return success(new ActivityAdminDTO.Id(service.publish(id)));
    }

    /** 下架活动，关闭新开团。 */
    @PostMapping("/activities/{id}/offline")
    public Response<Void> offline(@PathVariable long id) {
        service.offline(id);
        return success(null);
    }

    /** 保持拼团 API 的业务响应结构。 */
    @ExceptionHandler(AppException.class)
    public Response<Void> businessError(AppException exception) {
        return Response.<Void>builder().code(exception.getCode()).info(exception.getInfo()).build();
    }

    private ActivityDraftVO toDomain(ActivityAdminDTO.Draft draft) {
        List<GroupBuyTierVO> tiers = draft.tiers() == null ? List.of() : draft.tiers().stream()
                .map(tier -> tier == null ? null : new GroupBuyTierVO(tier.tierId(), tier.targetCount(), tier.memberPrice()))
                .toList();
        return new ActivityDraftVO(draft.name(), draft.goodsId(), draft.source(), draft.channel(),
                draft.startTime(), draft.endTime(), draft.validMinutes(), draft.userLimit(), tiers, draft.skipGroupEnabled(), draft.skipGroupPrice(), draft.eligibilityShopId());
    }

    private ActivityAdminDTO.Draft toDto(ActivityDraftVO draft) {
        List<ActivityAdminDTO.Tier> tiers = draft.tiers().stream().map(tier -> new ActivityAdminDTO.Tier(
                tier.tierId(), tier.targetCount(), tier.memberPrice(), GroupBuyPrice.leaderPrice(tier.memberPrice()))).toList();
        return new ActivityAdminDTO.Draft(draft.name(), draft.goodsId(), draft.source(), draft.channel(),
                draft.startTime(), draft.endTime(), draft.validMinutes(), draft.userLimit(), tiers, draft.skipGroupEnabled(), draft.skipGroupPrice(), draft.eligibilityShopId());
    }

    private ActivityAdminDTO.Activity toDto(ManagedActivityEntity activity) {
        return new ActivityAdminDTO.Activity(activity.id(), activity.activityId(), activity.state().name(), toDto(activity.draft()));
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder().code(ResponseCodeEnum.SUCCESS.getCode()).info(ResponseCodeEnum.SUCCESS.getInfo())
                .data(data).build();
    }
}
