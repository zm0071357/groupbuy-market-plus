package groupbuy.market.plus.infrastructure.dcc;

import groupbuy.market.plus.types.annotations.DCCValue;
import org.springframework.stereotype.Service;

@Service
public class DCCServiceImpl {

    // 降级开关
    // 0 关闭
    // 1 开启
    @DCCValue(value = "downgradeSwitch:0")
    private String downgradeSwitch;

    // 切量开关
    @DCCValue(value = "cutRange:50")
    private String cutRange;

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

}
