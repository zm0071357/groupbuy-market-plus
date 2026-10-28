package groupbuy.market.plus.infrastructure.dcc;

import groupbuy.market.plus.types.annotations.DCCValue;
import groupbuy.market.plus.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DCCServiceImpl {

    /**
     * 切量比例默认值 - 与 @DCCValue 默认配置一致，配置值非法时按全部放行处理
     */
    private static final int DEFAULT_CUT_RANGE = 100;

    /**
     * 最近一次告警的非法切量配置值 - 同一非法值只告警一次，避免每个请求都打印错误日志
     */
    private volatile String lastInvalidCutRange;

    // 降级开关
    // 0 关闭
    // 1 开启
    @DCCValue(value = "downgradeSwitch:0")
    private volatile String downgradeSwitch;

    // 切量开关
    @DCCValue(value = "cutRange:100")
    private volatile String cutRange;

    // SC黑名单
    @DCCValue(value = "scBlack:s02,c02")
    private volatile String scBlack;

    /**
     * 降级开关是否开启
     * @return
     */
    public boolean isDowngradeSwitch() {
        return "1".equals(downgradeSwitch);
    }

    /**
     * 用户是否在切量范围内
     * @param userId
     * @return
     */
    public boolean isCutRange(String userId) {
        // 用户ID哈希值对100取模，得到 [0, 99] 的分桶
        // 使用 floorMod 而非 Math.abs：Math.abs(Integer.MIN_VALUE) 仍为负数
        int bucket = Math.floorMod(userId.hashCode(), 100);
        // 分桶小于切量比例即放行：0 全部拦截，100 全部放行，N 放行 N%
        return bucket < getCutRange();
    }

    /**
     * 解析切量比例 - 动态配置可能被推送为非数字或越界的值，解析失败按默认值 100（全部放行）处理，越界时限定在 [0, 100]，
     * 避免非法配置导致所有试算、锁单请求抛出 NumberFormatException
     * @return 切量比例 [0, 100]
     */
    private int getCutRange() {
        String value = cutRange;
        try {
            int range = Integer.parseInt(value.trim());
            if (range >= 0 && range <= 100) {
                return range;
            }
            warnInvalidCutRange(value);
            return Math.max(0, Math.min(100, range));
        } catch (NumberFormatException | NullPointerException e) {
            warnInvalidCutRange(value);
            return DEFAULT_CUT_RANGE;
        }
    }

    private void warnInvalidCutRange(String value) {
        if (!Objects.equals(value, lastInvalidCutRange)) {
            lastInvalidCutRange = value;
            log.error("DCC 切量比例配置非法：{}，应为 0-100 的整数，非数字按 {} 处理，越界按边界值处理", value, DEFAULT_CUT_RANGE);
        }
    }

    /**
     * SC是否黑名单
     * @param resource 来源
     * @param channel 渠道
     * @return
     */
    public boolean isBlack(String resource, String channel) {
        String[] split = scBlack.split(Constants.SPLIT);
        List<String> scBlackList = Arrays.asList(split);
        return scBlackList.contains(resource) || scBlackList.contains(channel);
    }
}
