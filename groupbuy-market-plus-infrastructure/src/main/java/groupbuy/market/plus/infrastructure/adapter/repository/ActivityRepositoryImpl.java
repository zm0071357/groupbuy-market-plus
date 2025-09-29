package groupbuy.market.plus.infrastructure.adapter.repository;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.entity.IndexTeamEntity;
import groupbuy.market.plus.domain.activity.model.valobj.*;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.infrastructure.dao.*;
import groupbuy.market.plus.infrastructure.dao.po.*;
import groupbuy.market.plus.infrastructure.dcc.DCCServiceImpl;
import groupbuy.market.plus.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private GroupBuyTeamDao groupBuyTeamDao;

    @Resource
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;

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

    @Override
    public List<IndexTeamEntity> getUserTeam(Long activityId, String userId, String goodsId, String source, String channel, Integer userTeamCount) {
        // 查询用户的组队ID集合
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setActivityId(activityId);
        groupBuyTeamOrderReq.setUserId(userId);
        groupBuyTeamOrderReq.setGoodsId(goodsId);
        groupBuyTeamOrderReq.setSource(source);
        groupBuyTeamOrderReq.setChannel(channel);
        List<String> teamIdList = groupBuyTeamOrderDao.getUserTeamIdList(groupBuyTeamOrderReq);
        if (teamIdList == null || teamIdList.isEmpty()) {
            return null;
        }
        // 根据组队ID集合查询对应组队信息
        List<GroupBuyTeam> userTeamList = groupBuyTeamDao.getUserTeam(teamIdList, userTeamCount);
        if (userTeamList == null || userTeamList.isEmpty()) {
            return null;
        }
        List<IndexTeamEntity> indexTeamEntityList = new ArrayList<>();
        for (GroupBuyTeam groupBuyTeam : userTeamList) {
            indexTeamEntityList.add(IndexTeamEntity.builder()
                            .teamId(groupBuyTeam.getTeamId())
                            .targetCount(groupBuyTeam.getTargetCount())
                            .completeCount(groupBuyTeam.getCompleteCount())
                            .lockCount(groupBuyTeam.getLockCount())
                            .startTime(groupBuyTeam.getStartTime())
                            .endTime(groupBuyTeam.getEndTime())
                    .build());
        }
        return indexTeamEntityList;
    }

    @Override
    public List<IndexTeamEntity> getRandomTeam(Long activityId, String userId, String goodsId, String source, String channel, Integer randomTeamCount) {
        // 查询用户未参与的组队ID集合
        GroupBuyTeamOrder groupBuyTeamOrderReq = new GroupBuyTeamOrder();
        groupBuyTeamOrderReq.setActivityId(activityId);
        groupBuyTeamOrderReq.setUserId(userId);
        groupBuyTeamOrderReq.setGoodsId(goodsId);
        groupBuyTeamOrderReq.setSource(source);
        groupBuyTeamOrderReq.setChannel(channel);
        List<String> teamIdList = groupBuyTeamOrderDao.getRandomTeamIdList(groupBuyTeamOrderReq, randomTeamCount * 2);
        if (teamIdList == null || teamIdList.isEmpty()) {
            return null;
        }
        // 判断总数量是否大于所需数量
        if (teamIdList.size() > randomTeamCount) {
            // 随机打乱列表
            Collections.shuffle(teamIdList);
            // 截取到所需数量
            teamIdList = teamIdList.subList(0, randomTeamCount);
        }
        // 根据组队ID集合查询对应组队信息
        List<GroupBuyTeam> userTeamList = groupBuyTeamDao.getRandomTeam(teamIdList);
        if (userTeamList == null || userTeamList.isEmpty()) {
            return null;
        }
        List<IndexTeamEntity> indexTeamEntityList = new ArrayList<>();
        for (GroupBuyTeam groupBuyTeam : userTeamList) {
            indexTeamEntityList.add(IndexTeamEntity.builder()
                    .teamId(groupBuyTeam.getTeamId())
                    .targetCount(groupBuyTeam.getTargetCount())
                    .completeCount(groupBuyTeam.getCompleteCount())
                    .lockCount(groupBuyTeam.getLockCount())
                    .startTime(groupBuyTeam.getStartTime())
                    .endTime(groupBuyTeam.getEndTime())
                    .build());
        }
        return indexTeamEntityList;
    }

    @Override
    public StatisticsVO getStatistics(Long activityId, String goodsId) {
        GroupBuyTeam groupBuyTeamReq = new GroupBuyTeam();
        groupBuyTeamReq.setActivityId(activityId);
        groupBuyTeamReq.setGoodsId(goodsId);
        List<String> teamIdList = groupBuyTeamDao.getTeamIdList(groupBuyTeamReq);
        if (teamIdList == null || teamIdList.isEmpty()) {
            return StatisticsVO.builder()
                    .allTeamCount(0)
                    .allTeamCompleteCount(0)
                    .allTeamUserCount(0)
                    .build();
        }
        Integer allTeamCount = teamIdList.size();
        Integer allTeamCompleteCount = groupBuyTeamDao.getAllTeamCompleteCount(teamIdList);
        Integer allTeamUserCount = groupBuyTeamDao.getAllTeamUserCount(teamIdList);
        return StatisticsVO.builder()
                .allTeamCount(allTeamCount)
                .allTeamCompleteCount(allTeamCompleteCount)
                .allTeamUserCount(allTeamUserCount)
                .build();
    }

}
