package groupbuy.market.plus.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品页面响应体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IndexResponseDTO {

    /**
     * 商品信息
     */
    private Goods goods;

    /**
     * 拼团组队信息
     */
    private List<Team> teamList;

    /**
     * 统计信息
     */
    private Statistics statistics;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Goods {

        /**
         * 商品ID
         */
        private String goodsId;

        /**
         * 原始价格
         */
        private BigDecimal originalPrice;

        /**
         * 折扣价格
         */
        private BigDecimal deductionPrice;

        /**
         * 支付价格
         */
        private BigDecimal payPrice;
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Team {

        /**
         * 组队ID
         */
        private String teamId;

        /**
         * 目标数量
         */
        private Integer targetCount;

        /**
         * 完成数量
         */
        private Integer completeCount;

        /**
         * 锁单数量
         */
        private Integer lockCount;

        /**
         * 拼团开始时间
         */
        private Date startTime;

        /**
         * 拼团结束时间
         */
        private Date endTime;

        /**
         * 倒计时
         */
        private String validTimeCountdown;

        /**
         * 获取倒计时方法
         * @param startTime 拼团开始时间
         * @param endTime 拼团结束时间
         * @return
         */
        public static String getValidTimeCountdown(Date startTime, Date endTime) {
            if (startTime == null || endTime == null) {
                return "无效的时间";
            }
            // 获取秒
            long sec = endTime.getTime() - startTime.getTime();
            if (sec < 0) {
                return "已结束";
            }
            // 将秒转换为指定时间格式
            long seconds = TimeUnit.MILLISECONDS.toSeconds(sec) % 60;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(sec) % 60;
            long hours = TimeUnit.MILLISECONDS.toHours(sec) % 24;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Statistics {

        /**
         * 拼团数量
         */
        private Integer allTeamCount;

        /**
         * 拼团完成数量
         */
        private Integer allTeamCompleteCount;

        /**
         * 参与拼团总人数
         */
        private Integer allTeamUserCount;
    }

}
