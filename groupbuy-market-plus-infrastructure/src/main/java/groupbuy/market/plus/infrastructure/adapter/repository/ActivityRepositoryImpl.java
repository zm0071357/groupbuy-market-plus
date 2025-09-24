package groupbuy.market.plus.infrastructure.adapter.repository;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.DiscountTypeEnum;
import groupbuy.market.plus.domain.activity.model.valobj.SCSkuActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.infrastructure.dao.ActivityDao;
import groupbuy.market.plus.infrastructure.dao.DiscountDao;
import groupbuy.market.plus.infrastructure.dao.SCSkuActivityDao;
import groupbuy.market.plus.infrastructure.dao.SkuDao;
import groupbuy.market.plus.infrastructure.dao.po.Activity;
import groupbuy.market.plus.infrastructure.dao.po.Discount;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import groupbuy.market.plus.infrastructure.dao.po.Sku;
import groupbuy.market.plus.infrastructure.dcc.DCCServiceImpl;
import groupbuy.market.plus.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class ActivityRepositoryImpl implements ActivityRepository {

    @Resource
    private SkuDao skuDao;

    @Resource
    private ActivityDao activityDao;

    @Resource
    private SCSkuActivityDao scSkuActivityDao;

    @Resource
    private DiscountDao discountDao;

    @Resource
    private DCCServiceImpl dccServiceImpl;

    @Override
    public SkuVO getSkuByGoodsId(String goodsId) {
        Sku sku = skuDao.getSkuByGoodsId(goodsId);
        if (sku == null) {
            return null;
        }
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public ActivityVO getActivityById(Long activityId) {
        // 获取活动
        Activity activity = activityDao.getActivityById(activityId);
        if (activity == null) {
            return null;
        }
        // 获取折扣组合
        List<ActivityVO.GroupBuyDiscount> groupBuyDiscountList = new ArrayList<>();
        String discountExpr = activity.getDiscountExpr();
        String[] discountIds = discountExpr.split(Constants.SPLIT);
        for (String discountId : discountIds) {
            Discount discount = discountDao.getDiscountById(discountId);
            groupBuyDiscountList.add(ActivityVO.GroupBuyDiscount.builder()
                            .discountName(discount.getDiscountName())
                            .discountDesc(discount.getDiscountDesc())
                            .discountType(DiscountTypeEnum.get(discount.getDiscountType()))
                            .marketPlan(discount.getMarketPlan())
                            .marketExpr(discount.getMarketExpr())
                            .tagId(discount.getTagId())
                    .build());
        }
        return ActivityVO.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .groupBuyDiscountList(groupBuyDiscountList)
                .groupType(activity.getGroupType())
                .takeLimitCount(activity.getTakeLimitCount())
                .target(activity.getTarget())
                .validTime(activity.getValidTime())
                .status(activity.getStatus())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .tagId(activity.getTagId())
                .tagScope(activity.getTagScope())
                .build();
    }

    @Override
    public SCSkuActivityVO getSCSkuActivityByGoodsIdWithSC(String goodsId, String source, String channel) {
        SCSkuActivity scSkuActivityReq = new SCSkuActivity();
        scSkuActivityReq.setChannel(channel);
        scSkuActivityReq.setSource(source);
        scSkuActivityReq.setGoodsId(goodsId);
        SCSkuActivity scSkuActivity = scSkuActivityDao.getSCSkuActivityByIdWithSC(scSkuActivityReq);
        if (scSkuActivity == null) {
            return null;
        }
        return SCSkuActivityVO.builder()
                .source(scSkuActivity.getSource())
                .chanel(scSkuActivity.getChannel())
                .activityId(scSkuActivity.getActivityId())
                .goodsId(scSkuActivity.getGoodsId())
                .build();
    }

    @Override
    public boolean downgradeSwitch() {
        return dccServiceImpl.isDowngradeSwitch();
    }

    @Override
    public boolean cutRange(String userId) {
        return dccServiceImpl.isCutRange(userId);
    }

}
