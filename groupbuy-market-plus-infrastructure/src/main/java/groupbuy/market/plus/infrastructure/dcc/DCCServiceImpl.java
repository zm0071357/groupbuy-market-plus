package groupbuy.market.plus.infrastructure.dcc;

import groupbuy.market.plus.types.annotations.DCCValue;
import groupbuy.market.plus.types.common.Constants;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DCCServiceImpl {

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
        return bucket < Integer.parseInt(cutRange);
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
