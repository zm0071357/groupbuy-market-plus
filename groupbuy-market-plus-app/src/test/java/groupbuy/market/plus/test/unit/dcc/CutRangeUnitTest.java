package groupbuy.market.plus.test.unit.dcc;

import groupbuy.market.plus.infrastructure.dcc.DCCServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 切量判断单元测试 - 切量比例边界
 */
public class CutRangeUnitTest {

    /**
     * hashCode 为 Integer.MIN_VALUE 的字符串
     */
    private static final String MIN_HASH_USER = "polygenelubricants";

    @Test
    public void test_cutRange_boundary() {
        Assert.assertEquals(Integer.MIN_VALUE, MIN_HASH_USER.hashCode());

        // 切量 0：全部拦截（含哈希值为 Integer.MIN_VALUE 的用户）
        DCCServiceImpl dccService = buildService("0");
        for (int i = 0; i < 1000; i++) {
            Assert.assertFalse(dccService.isCutRange("user" + i));
        }
        Assert.assertFalse(dccService.isCutRange(MIN_HASH_USER));

        // 切量 100：全部放行
        dccService = buildService("100");
        for (int i = 0; i < 1000; i++) {
            Assert.assertTrue(dccService.isCutRange("user" + i));
        }
        Assert.assertTrue(dccService.isCutRange(MIN_HASH_USER));
    }

    @Test
    public void test_cutRange_exactPercent() {
        // 分桶 0~99 各取一个用户：切量 50 时恰好放行分桶 0~49
        DCCServiceImpl dccService = buildService("50");
        int pass = 0;
        for (int bucket = 0; bucket < 100; bucket++) {
            if (dccService.isCutRange(userOfBucket(bucket))) {
                pass++;
            }
        }
        Assert.assertEquals(50, pass);
        Assert.assertTrue(dccService.isCutRange(userOfBucket(49)));
        Assert.assertFalse(dccService.isCutRange(userOfBucket(50)));
    }

    @Test
    public void test_cutRange_invalidValue_noException() {
        // 非数字、空值 - 按默认值 100 全部放行，不抛 NumberFormatException
        for (String invalid : new String[]{"50%", "", " ", "abc", null}) {
            DCCServiceImpl dccService = buildService(invalid);
            for (int i = 0; i < 100; i++) {
                Assert.assertTrue("非法值 [" + invalid + "] 应全部放行", dccService.isCutRange("user" + i));
            }
        }
        // 首尾空白可正常解析
        Assert.assertFalse(buildService(" 0 ").isCutRange(userOfBucket(0)));
        // 越界 - 限定在 [0, 100]
        DCCServiceImpl over = buildService("150");
        DCCServiceImpl under = buildService("-5");
        for (int bucket = 0; bucket < 100; bucket++) {
            Assert.assertTrue(over.isCutRange(userOfBucket(bucket)));
            Assert.assertFalse(under.isCutRange(userOfBucket(bucket)));
        }
    }

    private DCCServiceImpl buildService(String cutRange) {
        DCCServiceImpl dccService = new DCCServiceImpl();
        ReflectionTestUtils.setField(dccService, "cutRange", cutRange);
        return dccService;
    }

    private String userOfBucket(int bucket) {
        for (int i = 0; ; i++) {
            String userId = "u" + i;
            if (Math.floorMod(userId.hashCode(), 100) == bucket) {
                return userId;
            }
        }
    }

}
