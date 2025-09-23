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
            return bigInt.mod(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
