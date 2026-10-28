package groupbuy.market.plus.test.unit.notify;

import groupbuy.market.plus.config.OkHttpClientConfig;
import groupbuy.market.plus.config.OkHttpClientProperties;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 超时与调度线程池配置单元测试
 */
public class TimeoutConfigUnitTest {

    @Test
    public void test_okHttp_defaultTimeout() throws Exception {
        // 未配置时也不能是 0（OkHttp 中 0 表示永不超时）
        OkHttpClient client = buildClient(new OkHttpClientProperties());
        Assert.assertEquals(5000, client.connectTimeoutMillis());
        Assert.assertEquals(10000, client.readTimeoutMillis());
        Assert.assertEquals(10000, client.writeTimeoutMillis());
    }

    @Test
    public void test_yml_bindTimeoutAndSchedulingPool() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application-dev", new ClassPathResource("application-dev.yml"));
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        Binder binder = Binder.get(environment);

        OkHttpClientProperties properties = binder.bind("okhttpclient.config", OkHttpClientProperties.class).get();
        Assert.assertEquals(5, properties.getConnectTimeOut());
        Assert.assertEquals(10, properties.getReadTimeOut());
        Assert.assertEquals(10, properties.getWriteTimeOut());
        Assert.assertEquals(Integer.valueOf(4), binder.bind("spring.task.scheduling.pool.size", Integer.class).get());
    }

    private OkHttpClient buildClient(OkHttpClientProperties properties) throws Exception {
        OkHttpClientConfig config = new OkHttpClientConfig();
        for (Method method : OkHttpClientConfig.class.getDeclaredMethods()) {
            if (method.getReturnType() == OkHttpClient.class) {
                method.setAccessible(true);
                return (OkHttpClient) method.invoke(config, properties);
            }
        }
        throw new IllegalStateException("未找到 OkHttpClient Bean 方法");
    }

}
