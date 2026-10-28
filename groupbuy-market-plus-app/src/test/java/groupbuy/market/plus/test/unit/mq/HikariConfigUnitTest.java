package groupbuy.market.plus.test.unit.mq;

import com.zaxxer.hikari.HikariConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

/**
 * Hikari 连接池配置单元测试 - 配置位于 spring.datasource.hikari 下才会绑定到数据源
 */
public class HikariConfigUnitTest {

    @Test
    public void test_hikari_bindUnderDatasource() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application-dev", new ClassPathResource("application-dev.yml"));
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        Binder binder = Binder.get(environment);

        // 与 Spring Boot DataSourceConfiguration.Hikari 相同的绑定前缀
        HikariConfig hikariConfig = binder.bind("spring.datasource.hikari", HikariConfig.class).get();
        Assert.assertEquals(25, hikariConfig.getMaximumPoolSize());
        Assert.assertEquals(15, hikariConfig.getMinimumIdle());
        Assert.assertEquals("Retail_HikariCP", hikariConfig.getPoolName());
        Assert.assertEquals("com.zaxxer.hikari.HikariDataSource", binder.bind("spring.datasource.type", String.class).get());
        // 旧的错误位置不再存在
        Assert.assertFalse(binder.bind("spring.hikari.maximum-pool-size", Integer.class).isBound());
    }

}
