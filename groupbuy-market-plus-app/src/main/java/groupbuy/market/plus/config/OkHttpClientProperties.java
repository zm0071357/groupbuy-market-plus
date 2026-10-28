package groupbuy.market.plus.config;

import lombok.Data;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "okhttpclient.config", ignoreInvalidFields = true)
public class OkHttpClientProperties {
    private HttpLoggingInterceptor.Level level = HttpLoggingInterceptor.Level.HEADERS;
    /**
     * 超时时间（秒）- OkHttp 中 0 表示永不超时，必须设置默认值，避免商户回调接口挂起时回调线程被无限阻塞
     */
    private long connectTimeOut = 5;
    private long writeTimeOut = 10;
    private long readTimeOut = 10;
}
