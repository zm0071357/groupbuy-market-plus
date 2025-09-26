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
    private String downgradeSwitch;

    // 切量开关
    @DCCValue(value = "cutRange:100")
    private String cutRange;

    // SC黑名单
    @DCCValue(value = "scBlack:s02,c02")
    private String scBlack;

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
        // 获取用户ID的哈希值
        int hashCode = Math.abs(userId.hashCode());
        // 取最后两位
        int lastTwoDigits = hashCode % 100;
        if (lastTwoDigits <= Integer.parseInt(cutRange)) {
            return true;
        }
        return false;
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
