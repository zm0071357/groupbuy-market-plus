package groupbuy.market.plus.types.utils;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 取模工具类
 */
@Component
public class ModuloUtil {

    /**
     * 人群标签 bitmap 下标范围 2^26 - 单个标签 bitmap 最大约 8MB
     * 原先对 Integer.MAX_VALUE 取模，下标接近 2^31，Redis 会为每个标签分配约 256MB
     */
    public static final int BITMAP_SIZE = 1 << 26;

    /**
     * 取模
     * @param userId
     * @return
     */
    public static int getIndexFromUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(userId.getBytes(StandardCharsets.UTF_8));
            // 将哈希字节数组转换为正整数
            BigInteger bigInt = new BigInteger(1, hashBytes);
            // 取模以确保索引在合理范围内
            return bigInt.mod(BigInteger.valueOf(BITMAP_SIZE)).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
