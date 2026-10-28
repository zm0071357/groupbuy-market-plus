package groupbuy.market.plus.domain.activity.model.valobj;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityVO {

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 折扣组合配置
     */
    private List<GroupBuyDiscount> groupBuyDiscountList;

    /**
     * 拼团方式（0自动成团、1达成目标拼团）
     */
    private Integer groupType;

    /**
     * 拼团次数限制
     */
    private Integer takeLimitCount;

    /**
     * 拼团目标
     */
    private Integer target;

    /**
     * 拼团时长（分钟）
     */
    private Integer validTime;

    /**
     * 活动状态（0创建、1生效、2过期、3废弃）
     */
    private Integer status;

    /**
     * 活动开始时间
     */
    private Date startTime;

    /**
     * 活动结束时间
     */
    private Date endTime;

    /**
     * 人群标签规则标识
     */
    private String tagId;

    /**
     * 人群标签规则范围 - 决定人群标签内的用户能否享受优惠（不限制参与拼团，不享受优惠的用户原价拼团）
     * 2,2 白名单：仅人群内用户享受优惠
     * 1,1 黑名单：人群内用户（如羊毛党）不享受优惠，人群外用户享受优惠
     * 其他取值或未配置视为配置错误，所有用户均不享受优惠
     */
    private String tagScope;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupBuyDiscount {

        /**
         * 折扣标题
         */
        private String discountName;

        /**
         * 折扣描述
         */
        private String discountDesc;

        /**
         * 折扣类型（0:base、1:tag）
         */
        private DiscountTypeEnum discountType;

        /**
         * 营销优惠计划（ZJ:直减、MJ:满减、N元购）
         */
        private String marketPlan;

        /**
         * 营销优惠表达式
         */
        private String marketExpr;

        /**
         * 人群标签，特定优惠限定
         */
        private String tagId;
    }

    /**
     * 人群范围 - 白名单：仅人群内用户享受优惠，人群外用户原价拼团
     */
    public static final String TAG_SCOPE_WHITE_LIST = "2,2";

    /**
     * 人群范围 - 黑名单：人群内用户（如羊毛党）原价拼团，人群外用户享受优惠
     */
    public static final String TAG_SCOPE_BLACK_LIST = "1,1";

    /**
     * 人群范围配置是否合法 - 人群标签只决定能否享受优惠，仅支持白名单 2,2 与黑名单 1,1 两种配置；
     * 其他取值（如 1,2、2,1）或配置了人群标签却未配置人群范围，均为配置错误
     * @return 是否合法
     */
    public boolean isValidTagScope() {
        String scope = StringUtils.deleteWhitespace(this.tagScope);
        return TAG_SCOPE_WHITE_LIST.equals(scope) || TAG_SCOPE_BLACK_LIST.equals(scope);
    }

    /**
     * 按人群范围判断用户能否享受优惠 - 调用前需先校验 {@link #isValidTagScope()}
     * 白名单：人群内用户享受优惠；黑名单：人群外用户享受优惠
     * @param isTagCrowdUser 用户是否在人群标签内
     * @return 是否享受优惠
     */
    public boolean isTagCrowdDiscount(boolean isTagCrowdUser) {
        boolean whiteList = TAG_SCOPE_WHITE_LIST.equals(StringUtils.deleteWhitespace(this.tagScope));
        return whiteList == isTagCrowdUser;
    }

}
